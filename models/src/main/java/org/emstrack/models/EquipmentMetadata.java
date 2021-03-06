package org.emstrack.models;

/**
 * A class representing an equipment metadata.
 * @author Fabian Choi
 * @since 5/4/2017
 */
public class EquipmentMetadata {
    private Integer id;
    private String name;
    private Character type;
    private boolean toggleable;

    public EquipmentMetadata(int id, String name,
                             Character type, boolean toggleable) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.toggleable = toggleable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Character getType() {
        return type;
    }

    public void setType(Character type) {
        this.type = type;
    }

    public boolean isToggleable() {
        return toggleable;
    }

    public void setToggleable(boolean toggleable) {
        this.toggleable = toggleable;
    }

}
