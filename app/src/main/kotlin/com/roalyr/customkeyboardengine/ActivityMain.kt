package com.roalyr.customkeyboardengine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roalyr.customkeyboardengine.ui.theme.CustomKeyboardEngineTheme
import java.io.File

/**
 * Main activity for the Custom Keyboard Engine.
 * Provides a UI for granting permissions, changing input methods, and copying default layouts/settings.
 */
class ActivityMain : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomKeyboardEngineTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()) // Enable scrolling
                            .padding(horizontal = 24.dp)
                            .padding(WindowInsets.systemBars.asPaddingValues()) // Respect notification bar
                            .imePadding()
                        , // Adjust for keyboard
                        verticalArrangement = Arrangement.spacedBy(8.dp), // Uniform spacing
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Spacer for Title
                        Spacer(modifier = Modifier.height(16.dp)) // Add initial padding for safe area



                        // Buttons
                        Button(
                            onClick = { requestStoragePermission() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.btn_grant_storage))
                        }

                        Button(
                            onClick = { requestOverlayPermission() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.btn_grant_overlay))
                        }

                        Button(
                            onClick = { openInputMethodSettings() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.btn_change_input))
                        }

                        Button(
                            onClick = { copyDefaults() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.btn_copy_defaults))
                        }

                        Button(
                            onClick = { copyDefaultSettings() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.btn_copy_settings))
                        }

                        // Path Reference
                        Text(
                            text = stringResource(R.string.label_working_dir),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(8.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.working_dir_path),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Hint Messages
                        Text(
                            text = stringResource(R.string.hint_json_files),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = stringResource(R.string.hint_directory),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Warning Message
                        Text(
                            text = stringResource(R.string.warning_backup),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Text Input Field
                        val text = remember { mutableStateOf("") }
                        androidx.compose.material3.TextField(
                            value = text.value,
                            onValueChange = { text.value = it },
                            placeholder = { Text(stringResource(R.string.hint_test_input)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        val pendingUri = remember { mutableStateOf<Uri?>(null) }
                        val pendingName = remember { mutableStateOf<String?>(null) }
                        val showReplaceDialog = remember { mutableStateOf(false) }
                        val showErrorDialog = remember { mutableStateOf<String?>(null) }

                        val importLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            if (uri != null) {
                                when (val r = this@ActivityMain.importLayoutFromUri(uri, false)) {
                                    is ImportResult.Success -> Toast.makeText(this@ActivityMain, "Загружено: " + r.fileName, Toast.LENGTH_SHORT).show()
                                    is ImportResult.Exists -> { pendingUri.value = uri; pendingName.value = r.fileName; showReplaceDialog.value = true }
                                    is ImportResult.Error -> { showErrorDialog.value = r.message }
                                }
                            }
                        }

                        Button(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Загрузить раскладку")
                        }

                        if (showReplaceDialog.value) {
                            AlertDialog(
                                onDismissRequest = { showReplaceDialog.value = false; pendingUri.value = null },
                                title = { Text("Заменить раскладку?") },
                                text = { Text("Раскладка " + (pendingName.value ?: "") + " уже существует. Заменить?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        pendingUri.value?.let { this@ActivityMain.importLayoutFromUri(it, true) }
                                        showReplaceDialog.value = false
                                        pendingUri.value = null
                                        Toast.makeText(this@ActivityMain, "Раскладка заменена", Toast.LENGTH_SHORT).show()
                                    }) { Text("Заменить") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showReplaceDialog.value = false; pendingUri.value = null }) { Text("Отмена") }
                                }
                            )
                        }

                        if (showErrorDialog.value != null) {
                            AlertDialog(
                                onDismissRequest = { showErrorDialog.value = null },
                                title = { Text("Ошибка") },
                                text = { Text(showErrorDialog.value ?: "") },
                                confirmButton = {
                                    TextButton(onClick = { showErrorDialog.value = null }) { Text("OK") }
                                }
                            )
                        }

                        // ---- STYLE SECTION ----
                        val stylePrefs = getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
                        val bgColorState = remember { mutableStateOf(stylePrefs.getString("custom_key_bg_color", "") ?: "") }
                        val textColorState = remember { mutableStateOf(stylePrefs.getString("custom_key_text_color", "") ?: "") }
                        val borderColorState = remember { mutableStateOf(stylePrefs.getString("custom_key_border_color", "") ?: "") }
                        val alphaState = remember { mutableStateOf(stylePrefs.getInt("custom_key_bg_alpha", 255).toFloat()) }
                        val borderWidthState = remember { mutableStateOf(stylePrefs.getInt("custom_key_border_width", 0).toFloat()) }
                        val modBgState = remember { mutableStateOf(stylePrefs.getString("custom_mod_bg_color", "") ?: "") }
                        val modAlphaState = remember { mutableStateOf(stylePrefs.getInt("custom_mod_bg_alpha", 255).toFloat()) }
                        val longPressState = remember { mutableStateOf(stylePrefs.getString("custom_longpress_color", "") ?: "") }
                        val modTextState = remember { mutableStateOf(stylePrefs.getString("custom_mod_text_color", "") ?: "") }

                        Text("Оформление клавиатуры", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

                        val colorPickerTarget = remember { mutableStateOf<String?>(null) }

                        Button(
                            onClick = { colorPickerTarget.value = "Цвет клавиш" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            val _c = if (bgColorState.value.isEmpty()) androidx.compose.ui.graphics.Color.Gray else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(bgColorState.value)) } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                            Box(modifier = Modifier.size(20.dp).background(_c, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Цвет клавиш" + (if (bgColorState.value.isEmpty()) " (авто)" else ""))
                        }

                        Button(
                            onClick = { colorPickerTarget.value = "Цвет букв" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            val _c = if (textColorState.value.isEmpty()) androidx.compose.ui.graphics.Color.Gray else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(textColorState.value)) } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                            Box(modifier = Modifier.size(20.dp).background(_c, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Цвет букв" + (if (textColorState.value.isEmpty()) " (авто)" else ""))
                        }

                        Button(
                            onClick = { colorPickerTarget.value = "Цвет контура" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            val _c = if (borderColorState.value.isEmpty()) androidx.compose.ui.graphics.Color.Gray else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(borderColorState.value)) } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                            Box(modifier = Modifier.size(20.dp).background(_c, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Цвет контура" + (if (borderColorState.value.isEmpty()) " (авто)" else ""))
                        }

                        Button(
                            onClick = { colorPickerTarget.value = "Цвет модификаторов" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            val _c = if (modBgState.value.isEmpty()) androidx.compose.ui.graphics.Color.Gray else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(modBgState.value)) } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                            Box(modifier = Modifier.size(20.dp).background(_c, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Цвет модификаторов" + (if (modBgState.value.isEmpty()) " (авто)" else ""))
                        }
                        Button(
                            onClick = { colorPickerTarget.value = "Цвет вторых символов" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            val _c = if (longPressState.value.isEmpty()) androidx.compose.ui.graphics.Color.Gray else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(longPressState.value)) } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                            Box(modifier = Modifier.size(20.dp).background(_c, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Цвет вторых символов" + (if (longPressState.value.isEmpty()) " (авто)" else ""))
                        }
                        Text("Прозрачность клавиш: " + alphaState.value.toInt() + "/255", fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = { colorPickerTarget.value = "Цвет символов модификаторов" },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            val _c = if (modTextState.value.isEmpty()) androidx.compose.ui.graphics.Color.Gray else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(modTextState.value)) } catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                            Box(modifier = Modifier.size(20.dp).background(_c, androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Цвет символов модификаторов" + (if (modTextState.value.isEmpty()) " (авто)" else ""))
                        }
                        Slider(value = alphaState.value, onValueChange = { alphaState.value = it }, valueRange = 0f..255f, modifier = Modifier.fillMaxWidth())

                        Text("Толщина контура: " + borderWidthState.value.toInt() + " px", fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
                        Slider(value = borderWidthState.value, onValueChange = { borderWidthState.value = it }, valueRange = 0f..20f, modifier = Modifier.fillMaxWidth())
                        Text("Прозрачность модификаторов: " + modAlphaState.value.toInt() + "/255", fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
                        Slider(value = modAlphaState.value, onValueChange = { modAlphaState.value = it }, valueRange = 0f..255f, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                stylePrefs.edit()
                                    .putString("custom_key_bg_color", bgColorState.value.trim())
                                    .putString("custom_key_text_color", textColorState.value.trim())
                                    .putString("custom_key_border_color", borderColorState.value.trim())
                                    .putInt("custom_key_bg_alpha", alphaState.value.toInt())
                                    .putInt("custom_key_border_width", borderWidthState.value.toInt())
                                    .putString("custom_mod_bg_color", modBgState.value.trim())
                                    .putInt("custom_mod_bg_alpha", modAlphaState.value.toInt())
                                    .putString("custom_longpress_color", longPressState.value.trim())
                                    .putString("custom_mod_text_color", modTextState.value.trim())
                                    .apply()
                                sendBroadcast(Intent("com.roalyr.customkeyboardengine.UPDATE_BACKGROUND"))
                                Toast.makeText(this@ActivityMain, "Стиль применён", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Применить стиль")
                        }

                        Button(
                            onClick = {
                                stylePrefs.edit()
                                    .putString("custom_key_bg_color", "")
                                    .putString("custom_key_text_color", "")
                                    .putString("custom_key_border_color", "")
                                    .putInt("custom_key_bg_alpha", 255)
                                    .putInt("custom_key_border_width", 0)
                                    .putString("custom_mod_bg_color", "")
                                    .putInt("custom_mod_bg_alpha", 255)
                                    .putString("custom_longpress_color", "")
                                    .putString("custom_mod_text_color", "")
                                    .apply()
                                bgColorState.value = ""
                                textColorState.value = ""
                                borderColorState.value = ""
                                alphaState.value = 255f
                                borderWidthState.value = 0f
                                modBgState.value = ""
                                modAlphaState.value = 255f
                                longPressState.value = ""
                                modTextState.value = ""
                                sendBroadcast(Intent("com.roalyr.customkeyboardengine.UPDATE_BACKGROUND"))
                                Toast.makeText(this@ActivityMain, "Стиль сброшен", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Сбросить стиль")
                        }

                        if (colorPickerTarget.value != null) {
                            val palette = listOf("", "#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#FFA500", "#800080", "#FFC0CB", "#808080", "#A52A2A", "#000080", "#008000", "#800000", "#808000", "#FFD700", "#4B0082", "#EE82EE", "#40E0D0", "#F5DEB3", "#2F4F4F", "#1E90FF", "#32CD32", "#FF6347", "#9370DB", "#20B2AA", "#DC143C")
                            AlertDialog(
                                onDismissRequest = { colorPickerTarget.value = null },
                                title = { Text("Выбор цвета: " + (colorPickerTarget.value ?: "")) },
                                text = {
                                    Column {
                                        palette.chunked(6).forEach { rowColors ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                                                rowColors.forEach { c ->
                                                    val displayColor = if (c.isEmpty()) androidx.compose.ui.graphics.Color.Transparent
                                                        else try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(c)) }
                                                        catch (e: Exception) { androidx.compose.ui.graphics.Color.Gray }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .background(displayColor, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                                            .clickable {
                                                                val v = colorPickerTarget.value
                                                                if (v == "Цвет клавиш") bgColorState.value = c
                                                                else if (v == "Цвет букв") textColorState.value = c
                                                                else if (v == "Цвет контура") borderColorState.value = c
                                                                else if (v == "Цвет модификаторов") modBgState.value = c
                                                                else if (v == "Цвет вторых символов") longPressState.value = c
                                                                else if (v == "Цвет символов модификаторов") modTextState.value = c
                                                                colorPickerTarget.value = null
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (c.isEmpty()) Text("авто", fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { colorPickerTarget.value = null }) { Text("Закрыть") }
                                }
                            )
                        }


                        val bgLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            if (uri != null) {
                                try {
                                    val dir = File(filesDir, "backgrounds")
                                    if (!dir.exists()) dir.mkdirs()
                                    val target = File(dir, "background.img")
                                    contentResolver.openInputStream(uri)?.use { inp ->
                                        target.outputStream().use { out -> inp.copyTo(out) }
                                    }
                                    sendBroadcast(Intent("com.roalyr.customkeyboardengine.UPDATE_BACKGROUND"))
                                    Toast.makeText(this@ActivityMain, "Фон установлен", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this@ActivityMain, "Ошибка: " + e.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        Button(
                            onClick = { bgLauncher.launch(arrayOf("image/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Выбрать фоновое фото")
                        }

                        Button(
                            onClick = {
                                val f = File(filesDir, "backgrounds/background.img")
                                if (f.exists()) { f.delete(); sendBroadcast(Intent("com.roalyr.customkeyboardengine.UPDATE_BACKGROUND")); Toast.makeText(this@ActivityMain, "Фон удалён", Toast.LENGTH_SHORT).show() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Сбросить фон")
                        }

                        // GitHub Link
                        val prefs = getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
                        val widthState = remember { mutableStateOf(prefs.getInt("float_width", 60).toString()) }
                        val heightState = remember { mutableStateOf(prefs.getInt("float_height", 40).toString()) }

                        OutlinedTextField(
                            value = widthState.value,
                            onValueChange = { widthState.value = it },
                            label = { Text(stringResource(R.string.label_float_width)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = heightState.value,
                            onValueChange = { heightState.value = it },
                            label = { Text(stringResource(R.string.label_float_height)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val w = widthState.value.toIntOrNull() ?: 60
                                val h = heightState.value.toIntOrNull() ?: 40
                                prefs.edit().putInt("float_width", w).putInt("float_height", h).apply()
                                sendBroadcast(Intent("com.roalyr.customkeyboardengine.UPDATE_SIZE"))
                                Toast.makeText(this@ActivityMain, getString(R.string.toast_size_applied), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.btn_apply_size))
                        }


                        Button(
                            onClick = { openUrl("https://github.com/roalyr/CustomKeyboardEngine") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(stringResource(R.string.btn_github))
                        }

                        Spacer(modifier = Modifier.height(16.dp)) // Add space at the bottom
                    }
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        ActivityPermissionRequest.startPermissionRequest(this, Constants.PermissionTypes.OVERLAY)
    }

    private fun requestStoragePermission() {
        ActivityPermissionRequest.startPermissionRequest(this, Constants.PermissionTypes.STORAGE)
    }


    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun openInputMethodSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun copyDefaults() {
        if (ClassFunctionsFiles.ensureMediaDirectoriesExistAndCopyDefaults(windowManager, this, resources)){
            Toast.makeText(this, getString(R.string.toast_defaults_copied), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies default settings.json from resources to the app working directory.
     *
     * Creates the media directory if it doesn't exist, then copies `R.raw.settings_default`
     * to `Android/media/com.roalyr.customkeyboardengine/settings.json`. If the file already exists,
     * it is overwritten.
     *
     * Shows toast notifications for success or failure.
     */
    private fun copyDefaultSettings() {
        val appDir = File(Constants.MEDIA_APP_DIRECTORY)
        if (!appDir.exists() && !appDir.mkdirs()) {
            Toast.makeText(this, getString(R.string.toast_folder_failed), Toast.LENGTH_SHORT).show()
            return
        }

        val targetFile = File(Constants.MEDIA_SETTINGS_FILE)
        try {
            resources.openRawResource(R.raw.settings_default).use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(this, getString(R.string.toast_settings_copied), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_settings_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    sealed class ImportResult {
        data class Success(val fileName: String) : ImportResult()
        data class Exists(val fileName: String) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    private fun importLayoutFromUri(uri: Uri, overwrite: Boolean): ImportResult {
        return try {
            var displayName: String? = null
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) displayName = cursor.getString(idx)
            }
            val rawName = displayName ?: "layout_${System.currentTimeMillis()}.json"
            val fileName = if (rawName.endsWith(".json", ignoreCase = true)) rawName else "$rawName.json"

            val targetDir = File(Constants.MEDIA_LAYOUTS_LANGUAGE_DIRECTORY)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                return ImportResult.Error("Не удалось создать папку раскладок")
            }
            val targetFile = File(targetDir, fileName)

            if (targetFile.exists() && !overwrite) {
                return ImportResult.Exists(fileName)
            }

            val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return ImportResult.Error("Не удалось прочитать файл")

            try {
                kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    .decodeFromString<KeyboardLayout>(content)
            } catch (e: Exception) {
                return ImportResult.Error("Неверный формат JSON: ${e.message}")
            }

            targetFile.writeText(content)
            ImportResult.Success(fileName)
        } catch (e: Exception) {
            ImportResult.Error("Ошибка: ${e.message}")
        }
    }
}
