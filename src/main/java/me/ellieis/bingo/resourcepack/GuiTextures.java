package me.ellieis.bingo.resourcepack;

import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.text.Text;

import java.util.function.Function;
import java.util.function.Supplier;

import static me.ellieis.bingo.Bingo.identifier;
import static me.ellieis.bingo.resourcepack.UiResourceCreator.*;

// literally all of this is stolen from nucleoid extras
public class GuiTextures {
    public static final Function<Text, Text> BINGO_CARD = background("bingo_card");;
    public static final Supplier<GuiElementBuilder> CLAIMED_SLOT = icon16("claimed_slot");
    public static final Supplier<GuiElementBuilder> LOCKED_SLOT = icon16("locked_slot");

    public static void register() {
        ResourcePackExtras.forDefault().addBridgedModelsFolder(identifier("sgui"));
        UiResourceCreator.setup();
    }

}