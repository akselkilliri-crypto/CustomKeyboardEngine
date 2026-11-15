# CustomKeyboardEngine — Project Notes

> Форк roalyr/CustomKeyboardEngine (GPL v3) с плавающим режимом, русской локализацией и кастомизацией.

## 📁 Структура проекта


app/src/main/
├── kotlin/com/roalyr/customkeyboardengine/
│   ├── ActivityMain.kt            # Главное меню приложения (Jetpack Compose): настройки, импорт раскладок, фон, цвета
│   ├── ActivityPermissionRequest.kt # Запрос разрешений (overlay, storage)
│   ├── CustomKeyboardService.kt   # IME-сервис: жизненный цикл клавиатуры, плавающий режим, буфер обмена
│   ├── CustomKeyboardView.kt      # Рендер клавиатуры (onDraw): цвета, прозрачность, фон, контуры, drag
│   ├── CustomKeyboard.kt          # Парсер JSON-раскладок (kotlinx.serialization), data-классы KeyboardLayout, Key
│   ├── CustomKeyboardClipboard.kt # Синглтон-история буфера обмена (Map<keyId, String>)
│   ├── ClassFunctionsFiles.kt     # Копирование встроенных раскладок в Android/media/.../layouts/
│   ├── ClassFunctionsPopups.kt    # Всплывающие ошибки
│   ├── ColorUtils.kt              # Парсинг hex-цветов
│   └── Constants.kt               # Ключевые константы: пути, коды клавиш (KEYCODE_CYCLE_LANGUAGE_LAYOUT=-21 и т.д.)
└── res/
    ├── raw/
    │   ├── keyboard_default.json          # Английская раскладка (шаблон)
    │   ├── keyboard_russian.json          # Русская ЙЦУКЕН (наша)
    │   ├── keyboard_service.json          # Служебные кнопки
    │   ├── keyboard_clipboard_default.json # Раскладка буфера (7 слотов)
    │   └── settings_default.json          # Дефолтные настройки
    ├── layout/                             # XML-разметки (floating_keyboard_view.xml, standard_keyboard_view.xml)
    ├── values/strings.xml                  # Английские строки
    └── values-ru/strings.xml               # Русские строки (наши)


## 🔧 Как это работает

### Раскладки (JSON)
- Формат: { "defaultKeyWidth": 10, "defaultKeyHeight": 45, "rows": [{ "keys": [{ "label": "q", "keyCode": 113, "labelLongPress": "!", "isModifier": false, "keyWidth": 10, "logicalKeyGap": 0 }] }] }
- Встроенные лежат в res/raw/, копируются в Android/media/com.roalyr.customkeyboardengine/layouts/ кнопкой «4. Скопировать раскладки»
- Языки переключаются циклически по кнопке en (keyCode: -21 = KEYCODE_CYCLE_LANGUAGE_LAYOUT)
- Импорт пользовательских раскладок: SAF-пикер в ActivityMain → валидация JSON через kotlinx.serialization → копирование в layouts/

### Плавающий режим
- Активируется кнопкой ⚓ (keyCode: -12 = KEYCODE_SWITCH_KEYBOARD_MODE)
- Использует WindowManager с TYPE_APPLICATION_OVERLAY + FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL
- Размер: 60% ширины × 40% высоты (настраивается в ActivityMain, prefs: float_width, float_height)
- onComputeInsets — приложение под клавиатурой НЕ сжимается (contentTopInsets = visibleTopInsets)
- Drag: setupDragListener() в CustomKeyboardService.kt, порог scaledTouchSlop * 3 (иначе тапы конфликтуют со свайпом)
- ВАЖНО: менять реальный LayoutParams.width/height, а НЕ scaleX/scaleY (иначе тач-зоны не совпадают с визуалом!)

### Буфер обмена
- Синглтон CustomKeyboardClipboard хранит Map<keyId, String> — история FIFO, свежее сверху
- Слоты в keyboard_clipboard_default.json — 7 клавиш с keyCode: -22
- ВАЖНО: updateClipboardMap и synchronizeClipboardKeys обходят ВСЕ три view (keyboardView, floatingKeyboardView, serviceKeyboardView) — если править, не забывать про плавающий режим

### Кастомизация (prefs keyboard_prefs)
- custom_key_bg_color — фон обычных клавиш (hex)
- custom_key_text_color — цвет букв
- custom_key_border_color — цвет контура
- custom_key_bg_alpha — прозрачность обычных клавиш (0-255)
- custom_key_border_width — толщина контура (0-20 px)
- custom_mod_bg_color — фон модификаторов (Esc/Del/Ctrl/Shift/Alt/Enter/стрелки)
- custom_mod_bg_alpha — прозрачность модификаторов
- custom_longpress_color — цвет вторых символов (появляются при long-press)
- float_width, float_height — размер плавающей клавиатуры в %
- Фон: Android/media/com.roalyr.customkeyboardengine/backgrounds/background.img (Bitmap, centerCrop + overlay 120 alpha)
- Применить стиль → sendBroadcast(Intent("com.roalyr.customkeyboardengine.UPDATE_BACKGROUND")) → сервис перерисовывает

## 🐛 ИЗВЕСТНЫЕ БАГИ (что нужно исправить)

### 🔴 Критично
1. Фон пропадает при начале печати — при нажатии на поле ввода и начале печати картинка фона заменяется серым цветом. Скорее всего проблема в onDraw: фон читается из prefs и Bitmap, но при invalidateAllKeysOnAllKeyboards() или при onStartInputView что-то сбрасывает bgBitmap. Также возможно кеш bgBitmapMtime не обновляется или FLAG_NOT_TOUCH_MODAL мешает. Начать с проверки: в CustomKeyboardView.onDraw после bgBitmap логировать, что путь и файл реально существуют при каждом тапе.
2. Цвет отдельных модификаторов — Shift/Ctrl/Alt/Enter/стрелки/Esc/Del имеют ОДИН общий цвет (keyModifierBackgroundColor). Нужно разделить на:
   - Стрелки (навигация) — отдельно
   - Shift/Ctrl/Alt (служебные модификаторы) — отдельно
   - Enter — отдельно
   - Esc/Del (действия) — отдельно
   Или дать пользователю выбор: «Цвет модификаторов» как сейчас + отдельно «Цвет стрелок». Реализация: различать по key.keyCode в onDraw.

### 🟡 Средний приоритет
3. Запятая в русской — сделана через longPress на . (ю) и / (.), но можно добавить прямую клавишу запятой в нижний ряд (сдвинуть буквы)
4. Ландшафтный режим — в горизонтали клавиатура занимает весь экран. Нужно ограничить высоту (например, 50% от экрана) или сделать плавающую по умолчанию
5. Emoji — кнопка есть (keyCode: -1), но не реализована панель эмодзи

### 🟢 Мелкие
6. keyboard_russian.json — буква ё отсутствует (только е). Можно добавить через long-press на е
7. Нет индикации активного языка на кнопке en (всегда показывает «en»)
8. values-ru/strings.xml — часть строк на английском (внутри CustomKeyboardView.kt жёстко зашиты английские надписи)

## 🚀 ROADMAP (что развить)

- [ ] Автозамена / словарь — T9 или просто словарь русских слов
- [ ] Жесты на пробеле — свайп влево/вправо для смены языка
- [ ] Настраиваемые раскладки — drag&drop клавиш в UI
- [ ] Экспорт/импорт настроек — JSON-файл со стилями
- [ ] Пресеты тем — тёмная/светлая/AMOLED + кастомные
- [ ] Звуки нажатий — SoundPool + настройка громкости
- [ ] Вибрация — HapticFeedbackConstants
- [ ] Прозрачный фон клавиатуры — слайдер для всей клавиатуры (не только клавиш)
- [ ] Градиент фона клавиш — linear/radial
- [ ] Свой цвет через hex — добавить в палитру «Custom» с полем ввода
- [ ] Мультиязычность раскладок — переключение по кнопке с индикацией текущего языка
- [ ] Синхронизация буфера — с системным ClipboardManager (уже есть listener, но нужна настройка «хранить историю N записей»)

## ⚙️ Сборка

- Локально: ./gradlew :app:assembleDebug (нужен Android SDK + JDK 17)
- CI: .github/workflows/build-v2.yml — GitHub Actions, артефакт custom-keyboard-debug
- Запуск сборки: git push origin HEAD:main (триггер на push)
- Скачать APK: gh run download <run-id> --repo akselkilliri-crypto/CustomKeyboardEngine --name custom-keyboard-debug --dir ~/keyboard_apk

## ⚠️ Правила работы с этим проектом

1. НЕ удалять LICENSE — GPL v3, обязательна
2. Пакет com.roalyr.customkeyboardengine — можно переименовать, но это большой рефакторинг
3. YAML в .github/workflows/ — только пробелы (2), не табы
4. При правке CustomKeyboardView.onDraw — не забывать про _customModBg, _customLongPress, bgBitmap
5. При правке буфера — обходить все три view (keyboardView, floatingKeyboardView, serviceKeyboardView)
6. При правке плавающего режима — НЕ использовать scaleX/scaleY (тач-зоны съедут)
7. Git push: если CI зависает в queued — создать НОВЫЙ workflow-файл (баг GitHub с ghost runs), не редактировать существующий
8. Тестирование: APK устанавливается поверх старой версии, prefs сохраняются

## 📊 Текущая версия

- v11 (последняя успешная сборка)
- Run ID: 43755476913
- Что работает: плавающий режим, drag, русский, буфер (синхронизация), фон, цвета (5 палитр), прозрачность (2 слайдера), контур, толщина
