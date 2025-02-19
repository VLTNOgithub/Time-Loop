package com.vltno.timeloop;

public class PlayerData {
    private String name;
    private String nickname;
    private String skin;

    public PlayerData(String name, String nickname, String skin) {
        this.name = name;
        this.nickname = nickname;
        this.skin = skin;
    }

    // Getters and setters for player attributes
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return "PlayerData{" +
                "name='" + name + '\'' +
                ", nickname='" + nickname + '\'' +
                ", skin='" + skin + '\'' +
                '}';
    }
}
