package wtf.moonlight.module.impl.display.island.impl;

import net.minecraft.client.Minecraft;
import wtf.moonlight.Client;
import wtf.moonlight.events.render.Shader2DEvent;
import wtf.moonlight.gui.font.Fonts;
import wtf.moonlight.module.impl.display.Interface;
import wtf.moonlight.module.impl.display.island.Island;
import wtf.moonlight.module.impl.display.island.IslandType;
import wtf.moonlight.util.render.RoundedUtil;

import java.awt.*;

import static wtf.moonlight.util.misc.InstanceAccess.mc;

public class InfoIsland extends Island {
    public InfoIsland() {
        super("Info", IslandType.INFO, 1000, getMiddleX() - 100, 20, 200, 20);
    }

    @Override
    public boolean shouldRender() {
        return false;
    }

    @Override
    public void render() {
        render(false);
    }

    @Override
    public void onShader(Shader2DEvent event) {
        render(true);
    }

    public void render(boolean shadow) {
        String text = Client.getCustomClientName() + " · " + mc.thePlayer.getName() + " · " + Minecraft.getDebugFPS() + "fps";

    }
}