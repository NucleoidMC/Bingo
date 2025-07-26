package me.ellieis.bingo.resourcepack;

import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static me.ellieis.bingo.Bingo.identifier;
import static me.ellieis.bingo.resourcepack.UiResourceCreator.*;

// literally all of this is stolen from nucleoid extras
public class GuiTextures {
    public static final Function<Text, Text> GAME_PORTAL_9X6 = background("game_portal_9x6");
    public static final Supplier<GuiElementBuilder> EMPTY_BUILDER = icon16("empty");
    public static final Supplier<GuiElementBuilder> NEXT_BUTTON = icon16("next");
    public static final Supplier<GuiElementBuilder> PREVIOUS_BUTTON = icon16("previous");
    public static final Supplier<GuiElementBuilder> BACK_BUTTON = icon16("back");

    public static final GuiElement EMPTY = EMPTY_BUILDER.get().hideTooltip().build();
    public static final char SPACE_1 = UiResourceCreator.space(1);

    public static void register() {
        ResourcePackExtras.forDefault().addBridgedModelsFolder(identifier("sgui"));
        UiResourceCreator.setup();
    }

}