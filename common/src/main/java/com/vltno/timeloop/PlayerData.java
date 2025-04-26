package com.vltno.timeloop;

import net.minecraft.world.phys.Vec3;

public class PlayerData {
    private String name;
    private String nickname;
    private String skin;
    private Vec3 startPosition;
    private Vec3 joinPosition;
    
    public PlayerData(String name, String nickname, String skin, Vec3 joinPosition) {
        this.name = name;
        this.nickname = nickname;
        this.skin = skin;
        this.startPosition = null;
        this.joinPosition = joinPosition;
    }

    // Getters and setters for player attributes
    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public Vec3 getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Vec3 newStartPosition) {
        this.startPosition = newStartPosition;
    }

    public Vec3 getJoinPosition() {
        return joinPosition;
    }
    
    public void setJoinPosition(Vec3 newJoinPosition) {
        this.joinPosition = newJoinPosition;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "name='" + name + '\'' +
                ", nickname='" + nickname + '\'' +
                ", skin='" + skin + '\'' +
                ", join-position='" + joinPosition + '\'' +
                ", start-position='" + startPosition + '\'' +
                '}';
    }
}
