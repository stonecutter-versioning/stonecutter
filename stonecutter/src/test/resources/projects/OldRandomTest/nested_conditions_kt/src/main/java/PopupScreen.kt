// Taken for testing from https://github.com/pupbrained/drop-confirm/blob/c77dfecc546198a6c67deecc18ea8fcf78319a45/src/main/kotlin/xyz/pupbrained/drop_confirm/screens/PopupScreen.kt
package projects.SyntaxTest.nested_conditions_2.src.main.kotlin

//? if >=1.20.1 {
import net.minecraft.client.gui.GuiGraphics as PoseStack
//?} elif >=1.16.5 {
/*import com.mojang.blaze3d.vertex.PoseStack
*///?}

//? if >=1.16.5 {
import net.minecraft.network.chat.Component as Text
//?} else {
/*import kotlin.String as Text
*///?}

class PopupScreen(val itemStack: ItemStack) : Screen(ComponentUtils.translatable("gui.drop_confirm")) {

  override fun render(/*? if >=1.16.5 {*/poseStack: PoseStack,/*?}*/ mouseX: Int, mouseY: Int, partialTick: Float) {
    getRenderImpl(/*? if >=1.16.5 {*/poseStack/*?}*/).apply {
      //? if <=1.20.1 {
      /*fillGradient(0, 0, width, height, DIMMING(), DIMMING())
      *///?} else if 1.20.4 {
      /*renderTransparentBackground(poseStack)
      *///?} else if <1.21.6 {
      /*renderBlurredBackground(/*? if <=1.21.1 {*/partialTick/*?}*/)
      *///?}
    }

    renderables.forEach { it.render(/*? if >=1.16.5 {*/poseStack,/*?}*/ mouseX, mouseY, partialTick) }
  }
}
