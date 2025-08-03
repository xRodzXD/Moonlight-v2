package wtf.moonlight.module.impl.display.island;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import wtf.moonlight.Client;
import wtf.moonlight.events.render.Shader2DEvent;
import wtf.moonlight.module.impl.display.Interface;
import wtf.moonlight.util.TimerUtil;

import java.awt.*;

@Getter
@Setter
public abstract class Island {
    private final String name;
    private final IslandType type;
    private final TimerUtil timerUtil = new TimerUtil();

    public long delay;
    public final float x, y, width, height;

    protected final Module module;
    protected Interface setting = Client.INSTANCE.getModuleManager().getModule(Interface.class);

    public Island(String name, IslandType type, long delay, float x, float y, float width, float height) {
        this.name = name;
        this.type = type;
        this.delay = delay;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.module = null;
    }

    public Island(String name, IslandType type, long delay, float x, float y, float width, int height, Module module) {
        this.name = name;
        this.type = type;
        this.delay = delay;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.module = module;
    }

    public abstract boolean shouldRender();

    public abstract void render();

    public abstract void onShader(Shader2DEvent event);

    public int getThemeColor() {
        return setting.color();
    }

    public int getBackgroundColor() {
        return setting.bgColor();
    }

    public int getThemeColor(int counter) {
        return setting.color(counter);
    }

    public int getBackgroundColor(int counter) {
        return setting.bgColor(counter);
    }

    public static float getMiddleX() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        return (float) sr.getScaledWidth_double();
    }
}