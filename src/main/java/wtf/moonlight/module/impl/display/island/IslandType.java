package wtf.moonlight.module.impl.display.island;

public enum IslandType {
    INFO("A","Info"),
    SCAFFOLD("B","Scaffold"),
    TABLIST("C","TabList"),
    SHORT("D","Short"),
    LONG("E","Long"),
    SPECIAL("S","Special");

    public final String icon, name;

    IslandType(String icon, String name) {
        this.icon = icon;
        this.name = name;
    }
}
