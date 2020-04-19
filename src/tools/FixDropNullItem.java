/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import database.DBConPool;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.MapleItemInformationProvider;

/**
 *
 * @author Pungin
 */
public class FixDropNullItem {

    private List<Integer> loadFromDB(int type) {
        List<Integer> dropid = new ArrayList<>();
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT itemid FROM drop_data ORDER BY itemid");
            if (type == 1) {
                sb.append(" DESC");
            }
            PreparedStatement ps = con.prepareStatement(sb.toString());
            ResultSet rs = ps.executeQuery();
            int itemId = 0;
            while (rs.next()) {
                try {
                    itemId = rs.getInt("itemid");
                } catch (Exception ex) {

                }
                if (itemId != 0) {
                    dropid.add(itemId);
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("無法載入掉落物");
            FileoutputUtil.outError("logs/資料庫異常.txt", e);
        }
        return dropid;
    }

    private void FixDropData(int type, int itemId) {

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT itemid, dropperid FROM drop_data WHERE itemid = ? ORDER BY itemid");
        if (type == 1) {
            sb.append(" DESC");
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement(sb.toString());
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (!ii.itemExists(itemId)) {
                    System.out.println("道具: " + MapleItemInformationProvider.getInstance().getName(itemId) + " 道具ID: " + itemId + " 怪物ID: " + rs.getInt("dropperid") + " 不存在，已從資料庫移除");
                    try {
                        PreparedStatement pp = con.prepareStatement("Delete From drop_data WHERE itemid = ?");
                        pp.setInt(1, itemId);
                        pp.executeUpdate();
                        pp.close();
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.out.println("處理掉落物失敗, 道具ID:" + itemId);
            FileoutputUtil.outError("logs/資料庫異常.txt", e);
        }
    }

    public static void main(String[] args) {
        System.out.println("請輸入種類，0為降逆排列 1為升冪排列");
        int type = 0;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            type = Integer.parseInt(br.readLine());
        } catch (IOException ex) {
            Logger.getLogger(FixDropNullItem.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (type > 1 || type < 0) {
            type = 0;
        }
        //System.setProperty("wzpath", System.getProperty("wzpath"));
        FixDropNullItem i = new FixDropNullItem();
        System.out.println("正在加載道具數據......");
        MapleItemInformationProvider.getInstance().load();
        System.out.println("正在讀取掉落物品......");
        List<Integer> list = i.loadFromDB(type);
        System.out.println("正在處理不存在之掉落物......， 種類為 : " + type);
        for (int ii : list) {
            i.FixDropData(type, ii);
        }
        System.out.println("處理不存在之掉落物結束。");
    }
}
