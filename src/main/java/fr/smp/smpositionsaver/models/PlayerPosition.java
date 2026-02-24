package fr.smp.smpositionsaver.models;

public class PlayerPosition {
    
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    
    public PlayerPosition(Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }
    
    public PlayerPosition(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getZ() {
        return z;
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    @Override
    public String toString() {
        return String.format("PlayerPosition{world=%s, x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f}", 
                worldName, x, y, z, yaw, pitch);
    }
}
