import re

p = 'app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardService.kt'
with open(p, 'r') as f:
s = f.read()

1. Добавляем импорты

if 'import android.view.MotionEvent' not in s:
s = s.replace('import android.view.WindowManager', 'import android.view.WindowManager\nimport android.view.MotionEvent\nimport android.view.ViewConfiguration\nimport kotlin.math.abs')

2. Удаляем scaleX/scaleY

s = re.sub(r'windowManager.addViewfloatingKeyboardView, params\sfloatingKeyboardView\?.scaleX = 0.6f\sfloatingKeyboardView\?.scaleY = 0.6f', 'windowManager.addView(floatingKeyboardView, params)', s)

3. Ширина 60%

s = s.replace(
'if (floatingKeyboardWidth == 0) {\n                floatingKeyboardWidth = getScreenWidth()',
'if (floatingKeyboardWidth == 0) {\n                floatingKeyboardWidth = (getScreenWidth() * 0.6).toInt()'
)

4. Начальная позиция

old_p = 'val params = WindowManager.LayoutParams(\n            floatingKeyboardWidth,\n            WindowManager.LayoutParams.WRAP_CONTENT,\n            floatingKeyboardPosX,'
new_p = '''if (floatingKeyboardPosX == 0) {
floatingKeyboardPosX = (screenWidth - floatingKeyboardWidth) / 2
}
if (floatingKeyboardPosY == 0) {
floatingKeyboardPosY = (screenHeight * 0.55).toInt()
}

s = s.replace(old_p, new_p)

5. Вызов setupDragListener

s = s.replace(
'floatingKeyboardView?.post { floatingKeyboardHeight = floatingKeyboardView?.measuredHeight ?: 0 }',
'setupDragListener()\n            floatingKeyboardView?.post { floatingKeyboardHeight = floatingKeyboardView?.measuredHeight ?: 0 }'
)

6. Функция перетаскивания

drag_func = '''private fun setupDragListener() {
val view = floatingKeyboardView ?: return
var downRawX = 0f
var downRawY = 0f
var initialX = 0
var initialY = 0
var isDragging = false
val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

s = s.replace('private fun createStandardKeyboard(): View? {', drag_func)

with open(p, 'w') as f:
f.write(s)
print('SUCCESS: Patch applied.')
