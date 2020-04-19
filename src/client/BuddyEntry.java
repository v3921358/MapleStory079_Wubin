/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import database.DBConPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.FileoutputUtil;

/**
 * 儲存好友個別的單位
 *
 * @author Flower
 */
public class BuddyEntry {

    /**
     * 好友名稱
     */
    private final String name;

    /**
     * 好友所在群組
     */
    private String group;

    /**
     * 好友ID
     */
    private final int characterId;

    /**
     * 好友等級
     */
    private final int level;

    /**
     * 好友職業
     */
    private final int job;

    /**
     * 好友可見度
     */
    private boolean visible;

    /**
     * 好友頻道
     */
    private int channel;

    /**
     * 建構子
     *
     * @param name 好友角色名稱
     * @param characterId 好友角色ID
     * @param group 好友所在群組
     * @param channel 角色所在頻道，離線則 -1
     * @param visible 好友是否可見
     * @param job 好友角色職業
     * @param level 好友角色等級
     */
    public BuddyEntry(String name, int characterId, String group, int channel, boolean visible, int level, int job) {
        super();
        this.name = name;
        this.characterId = characterId;
        this.group = group;
        this.channel = channel;
        this.visible = visible;
        this.level = level;
        this.job = job;
    }

    /**
     * @return 傳回好友角色所在的頻道，如果離線的話則 -1 returns -1.
     */
    public int getChannel() {
        return channel;
    }

    /**
     * 設定好友所在頻道
     *
     * @param channel 想要設定的頻道
     */
    public void setChannel(int channel) {
        this.channel = channel;
    }

    /**
     * 好友是否在線上
     *
     * @return 回傳好友是不是在線上
     */
    public boolean isOnline() {
        return channel >= 0;
    }

    /**
     * 設定好友已經離線
     */
    public void setOffline() {
        channel = -1;
    }

    /**
     * 取得好友名稱
     *
     * @return 好友名稱
     */
    public String getName() {
        return name;
    }

    /**
     * 取得好友的角色ID
     *
     * @return 好友角色ID
     */
    public int getCharacterId() {
        return characterId;
    }

    /**
     * 取得好友等級
     *
     * @return 好友的等級
     */
    public int getLevel() {
        return level;
    }

    /**
     * 取得好友的職業
     *
     * @return 好友的職業
     */
    public int getJob() {
        return job;
    }

    /**
     * 設定好友是不是可見的
     *
     * @param visible 可顯示上線與否
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * 回傳好友是不是可以顯示的
     *
     * @return 好友是否可見
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * 設定好友所在的群組
     *
     * @return 群組名稱
     */
    public String getGroup() {
        return group;
    }

    /**
     * 設定好友所在的群組
     *
     * @param newGroup 新群組名稱
     */
    public void setGroup(String newGroup) {
        this.group = newGroup;
    }

    public static BuddyEntry getByNameFromDB(String buddyName) {
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT id, name, level, job FROM characters WHERE name = ?");
            ps.setString(1, buddyName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new BuddyEntry(
                        rs.getString("name"),
                        rs.getInt("id"),
                        BuddyList.DEFAULT_GROUP,
                        -1,
                        false,
                        rs.getInt("level"),
                        rs.getInt("job"));
            } else {
                return null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            FileoutputUtil.outError("logs/資料庫異常.txt", ex);
            return null;
        }
    }

    public static BuddyEntry getByIdfFromDB(int buddyCharId) {
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT id, name, level, job FROM characters WHERE id = ?");
            ps.setInt(1, buddyCharId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new BuddyEntry(
                        rs.getString("name"),
                        rs.getInt("id"),
                        BuddyList.DEFAULT_GROUP,
                        -1,
                        true,
                        rs.getInt("level"),
                        rs.getInt("job"));
            } else {
                return null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            FileoutputUtil.outError("logs/資料庫異常.txt", ex);
            return null;
        }
    }

    /**
     * 哈希值
     *
     * @return 整數的哈希值
     */
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + characterId;
        return result;
    }

    /**
     * 判斷是否為同一個好友
     *
     * @param obj 欲傳入的物件
     * @return 是否一樣
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        final BuddyEntry other = (BuddyEntry) obj;
        return characterId == other.characterId;
    }
}
