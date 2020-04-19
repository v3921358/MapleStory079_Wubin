/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import handling.channel.ChannelServer;
import handling.world.World;
import java.awt.Point;
import server.maps.MapleMap;
import static tools.FileoutputUtil.CurrentReadable_Time;
import tools.MaplePacketCreator;

/**
 *
 * @author Administrator
 */
public class 活动神秘商人 {

    /**
     * <神秘商人代码>
     */
    //神秘商人代码
    public static int 神秘商人id = 9900001;
    public static int 神秘商人 = 0;
    //商人出现后存在的时间/分
    public static int 存在时间 = 5;
    public static int 神秘商人线程 = 0;
    public static int 神秘商人频道 = 0;
    public static int 神秘商人地图 = 0;
    public static int 神秘商人坐标X = 0;
    public static int 神秘商人坐标Y = 0;
    public static int 神秘商人时间 = 0;

    /**
     * <启动服务端的时候启动线程>
     */
    public static void 启动神秘商人() {
        随机商人出现条件();
    }

    /**
     * <随机出神秘商人出现的时间，地图，坐标>
     */
    public static void 随机商人出现条件() {
        //随机频道
        int pind = (int) Math.ceil(Math.random() * Integer.parseInt(ServerProperties.getProperty("server.settings.channel.count")));
        //如果随机的频道为0，则加1
        if (pind == 0) {
            pind += 1;
        }
        //随机时间
        int huor = (int) Math.ceil(Math.random() * 23);
        if (神秘商人时间 == huor) {
            if (huor == 23) {
                huor -= 1;
            } else {
                huor += 1;
            }
        }
        //随机坐标组
        int rand = (int) Math.ceil(Math.random() * 52);
        //随机地图坐标
        int[][] 坐标 = {
            {106010000, 488, 215},
            {240010000, 905, -298},
            {240010100, 2098, -508},
            {240010200, 2920, -688},
            {240010200, 409, -688},
            {240010300, -679, -1048},
            {240010300, 222, -868},
            {240010300, 719, 32},
            {110000000, -64, 151},
            {110000000, 379, -143},
            {110010000, -1594, -113},
            {110010000, 1204, -473},
            {110020000, -1077, -113},
            {110020000, -310, -118},
            {110020000, 1167, 182},
            {110030000, -1558, 149},
            {110030000, 164, 173},
            {104040000, 1059, -687},
            {104040000, 48, -685},
            {104030000, -858, -385},
            {104030000, 1481, -985},
            {104020000, 1418, -1345},
            {104010000, 2329, -115},
            {104010002, -1401, -25},
            {100000002, -16, -475},
            {100000002, 214, -475},
            {100010000, 198, 505},
            {100030000, -3652, -205},
            {100040000, 349, 1752},
            {105050000, 2282, 1619},
            {230010300, 44, 40},
            {230010300, -1744, -320},
            {541020100, 958, -346},
            {105090301, 928, -923},
            {105040305, 1245, 2295},
            {100030000, -2977, -1465},
            {100000006, -705, 215},
            {220030000, 638, 162},
            {600000000, 5682, -632},
            {200070000, -132, -715},
            {222010201, 120, -1047},
            {100040104, 66, 812},
            {260010400, 199, -85},
            {103010000, -1088, 232},
            {230010201, -50, -17},
            {240020100, -889, -508},
            {221020000, 11, 2162},
            {105090700, 523, -181},
            {220070201, 811, 1695},
            {211040300, -289, 454},
            {541010040, 1075, -1695},
            {600020300, 1, -204},
            {261000001, -47, 64}
        };
        //给予随机的值
        神秘商人时间 = huor;
        神秘商人频道 = pind;
        神秘商人地图 = 坐标[rand][0];
        神秘商人坐标X = 坐标[rand][1];
        神秘商人坐标Y = 坐标[rand][2];
        //召唤的频道
        ChannelServer channelServer = ChannelServer.getInstance(神秘商人频道);
        //召唤的地图
        MapleMap mapleMap = channelServer.getMapFactory().getMap(神秘商人地图);
        //通知信息
        String 信息 = "[神秘商人] : 一个神秘的商人 " + 神秘商人时间 + " 时将出现在 " + 神秘商人频道 + " 频道的某个地方。";
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, 信息));
        System.err.println("[服务端]" + CurrentReadable_Time() + " : " + 信息);
        System.err.println("[服务端]" + CurrentReadable_Time() + " : 出现在地图: " + 坐标[rand][0] + "( " + mapleMap.getMapName() + " ) 坐标: " + 坐标[rand][1] + "/" + 坐标[rand][2]);
    }

    /**
     * <满足条件后召唤神秘商人出现>
     */
    public static void 召唤神秘商人() {
        //召唤的频道
        ChannelServer channelServer = ChannelServer.getInstance(神秘商人频道);
        //召唤的地图
        MapleMap mapleMap = channelServer.getMapFactory().getMap(神秘商人地图);
        //召唤商人
        mapleMap.spawnNpc(神秘商人id, new Point(神秘商人坐标X, 神秘商人坐标Y));
        //延时执行
        String 信息 = "[神秘商人] : 一个神秘的商人出现在 " + 神秘商人频道 + " 频道" + mapleMap.getMapName() + "。";
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, 信息));
        System.err.println("[服务端]" + CurrentReadable_Time() + " : " + 信息);
        神秘商人 += 1;
        new Thread() {
            @Override
            public void run() {
                try {
                    //设置到时后清理商人
                    Thread.sleep(1000 * 60 * 存在时间);
                    删除神秘商人();
                } catch (InterruptedException e) {
                }
            }
        }.start();
    }

    /**
     * <删除神秘商人并且重置线程，并且随机下一次出现的时间地图和坐标>
     */
    public static void 删除神秘商人() {
        //召唤的频道
        ChannelServer channelServer = ChannelServer.getInstance(神秘商人频道);
        //召唤的地图
        MapleMap mapleMap = channelServer.getMapFactory().getMap(神秘商人地图);
        //删除商人
        mapleMap.removeNpc(神秘商人id);
        //重置商人线程
        神秘商人 = 0;
        //重新随机商人数据
        随机商人出现条件();
    }
}
