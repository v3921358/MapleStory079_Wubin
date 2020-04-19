/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.messages.commands;

import client.MapleCharacter;
import database.DBConPool;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import scripting.NPCScriptManager;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.CashItemFactory;
import server.FishingRewardFactory;
import server.MapleShopFactory;
import server.ShutdownServer;
import server.Timer;
import server.life.MapleMonsterInformationProvider;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.StringUtil;

/**
 *
 * @author Flower
 */
public class ConsoleCommand {

    /*public static class Info extends ConsoleCommandExecute {

        @Override
        public int execute(String[] paramArrayOfString) {
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            Runtime runtime = Runtime.getRuntime();

            NumberFormat format = NumberFormat.getInstance();

            StringBuilder sb = new StringBuilder();
            Long maxMemory = runtime.maxMemory();
            Long allocatedMemory = runtime.totalMemory();
            Long freeMemory = runtime.freeMemory();
            System.out.println("------------------ 系統資訊 ------------------");
            System.out.println("線程數 :" + ((Integer) threadSet.size()).toString());
            //System.out.println("SQL連接數 :" + ((Integer) DatabaseConnection.getConnectionsCount()).toString());
            System.out.println("記憶體最大限制 :" + Math.round(maxMemory / 1024 / 1024) + "Gb");
            System.out.println("已申請記憶體 :" + allocatedMemory.toString());
            System.out.println("尚未使用記憶體 :" + freeMemory.toString());

            return 1;
        }

    }*/

 /*public static class Shutdown extends ConsoleCommandExecute {

        private static Thread t = null;

        @Override
        public int execute(String[] splitted) {
            System.out.println("執行關閉作業");
            // for (handling.channel.ChannelServer cserv : handling.channel.ChannelServer.getAllInstances()) {
            //     cserv.closeAllMerchant();
            // }
            // System.out.println("精靈商人儲存完畢.");
            System.out.println("伺服器關閉中...");
            if (t == null || t.isAlive()) {
                try {
                    t = new Thread(server.ShutdownServer.getInstance());
                    t.start();

                } catch (Exception ex) {
                    Logger.getLogger(ConsoleCommand.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                System.out.println("已在執行中...");
            }
            return 1;

        }
    }*/

 /*public static class ShutdownTime extends ConsoleCommandExecute {

        private int minutesLeft = 0;
        private static Thread t = null;
        private static ScheduledFuture<?> ts = null;

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 1) {
                minutesLeft = Integer.parseInt(splitted[1]);
                if (ts == null && (t == null || !t.isAlive())) {
                    t = new Thread(server.ShutdownServer.getInstance());
                    ts = Timer.EventTimer.getInstance().register(new Runnable() {
                        @Override
                        public void run() {
                            if (minutesLeft == 1) {
                                for (handling.channel.ChannelServer cserv : handling.channel.ChannelServer.getAllInstances()) {
                                    //    cserv.closeAllMerchant();
                                }
                                System.out.println("精靈商人儲存完畢.");
                            } else if (minutesLeft == 0) {
                                ShutdownServer.getInstance().run();
                                t.start();
                                ts.cancel(false);
                                return;
                            }
                            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[楓之谷公告] 伺服器將在 " + minutesLeft + " 分鐘後關閉，請勿關閉精靈商人並存檔並下線。."));
                            System.out.println("伺服器將在 " + minutesLeft + "分鐘後關閉.");
                            minutesLeft--;
                        }
                    }, 60000);
                } else {
                    System.out.println("好吧真拿你沒辦法..伺服器關閉時間修改...請等待關閉完畢..請勿強制關閉服務器..否則後果自負!");
                }
            } else {
                System.out.println("使用規則: shutdowntime <關閉時間>");
                return 0;
            }
            return 1;
        }
    }*/
 /*public static class Dodown extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE loggedin = 1")) {
                    ps.executeUpdate();
                }
                System.out.println("所有帳號解卡完畢");
            } catch (SQLException ex) {
                FileoutputUtil.outError("logs/資料庫異常.txt", ex);
                System.err.println("解卡異常請查看MYSQL");
            }
            return 1;
        }
    }*/

 /*public static class ExpRate extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 2) {
                int rate = 1;
                try {
                    rate = Integer.parseInt(splitted[1]);
                } catch (Exception ex) {

                }
                if (splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setExpRate(rate);
                    }
                } else {
                    int channel = Integer.parseInt(splitted[2]);
                    ChannelServer.getInstance(channel).setExpRate(rate);
                }
                System.out.println("經驗倍率已改為 " + rate + "x");
            } else {
                System.out.println("Syntax: exprate <number> [<channel>/all]");
            }
            return 1;
        }
    }

    public static class DropRate extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 2) {
                int rate = 1;
                try {
                    rate = Integer.parseInt(splitted[1]);
                } catch (Exception ex) {

                }
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setDropRate(rate);
                    }
                } else {
                    int channel = Integer.parseInt(splitted[2]);
                    ChannelServer.getInstance(channel).setDropRate(rate);
                }
                System.out.println("掉落倍率已改為 " + rate + "x");
            } else {
                System.out.println("Syntax: droprate <number> [<channel>/all]");
            }
            return 1;
        }
    }

    public static class MesoRate extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 2) {
                int rate = 1;
                try {
                    rate = Integer.parseInt(splitted[1]);
                } catch (Exception ex) {

                }
                if (splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setMesoRate(rate);
                    }
                } else {
                    int channel = Integer.parseInt(splitted[2]);
                    ChannelServer.getInstance(channel).setMesoRate(rate);
                }
                System.out.println("金錢倍率已改為 " + rate + "x");
            } else {
                System.out.println("Syntax: mesorate <number> [<channel>/all]");
            }
            return 1;
        }
    }

    public static class Saveall extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            int p = 0;
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                List<MapleCharacter> chrs = cserv.getPlayerStorage().getAllCharactersThreadSafe();
                for (MapleCharacter chr : chrs) {
                    p++;
                    chr.saveToDB(false, true);
                }
            }
            System.out.println("[保存] " + p + "個玩家數據保存到數據中.");
            return 1;
        }
    }

    public static class AutoReg extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            LoginServer.setAutoReg(!LoginServer.getAutoReg());
            System.out.println("自動註冊狀態: " + (LoginServer.getAutoReg() ? "開啟" : "關閉"));
            return 1;
        }
    }

    public static class serverMsg extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    ch.setServerMessage(sb.toString());
                }
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverMessage(sb.toString()));
            } else {
                System.out.println("指令規則: !serverMsg <message>");
                return 0;
            }
            return 1;
        }
    }

    public static class CrucialTime extends ConsoleCommandExecute {

        protected static ScheduledFuture<?> ts = null;

        public int execute(String[] splitted) {
            if (splitted.length < 1) {
                return 0;
            }
            if (ts != null) {
                ts.cancel(false);
                System.out.println("原定的關鍵時刻已取消");
            }
            int minutesLeft = 0;
            try {
                minutesLeft = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException ex) {
                return 0;
            }
            if (minutesLeft > 0) {
                ts = Timer.EventTimer.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                                if (mch.getLevel() >= 29) {
                                    NPCScriptManager.getInstance().start(mch.getClient(), 9010010);
                                }
                            }
                        }
                        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "關鍵時刻開放囉，沒有30等以上的玩家是得不到的。"));
                        ts.cancel(false);
                        ts = null;
                    }
                }, minutesLeft * 60000);
                System.out.println("關鍵時刻預定已完成");
            } else {
                System.out.println("設定的時間必須 > 0");
            }
            return 1;
        }
    }

    public static class ReloadChannel extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {

            if (splitted[1].equalsIgnoreCase("all")) {
                for (ChannelServer csrv : ChannelServer.getAllInstances()) {
                    csrv.closeAllMerchant();
                    csrv.shutdown();
                }
                ChannelServer.startAllChannels();
                System.out.println("所有頻道重新讀取成功");
            } else {
                try {
                    final int channel = Integer.parseInt(splitted[1]);
                    ChannelServer csrv = ChannelServer.getInstance(channel);
                    csrv.closeAllMerchant();
                    ChannelServer.startChannel(channel);
                } catch (Exception e) {
                    System.out.println("[指令用法] reloadChannle <頻道/all>");
                }
            }
            return 1;
        }
    }

    public static class ReloadMap extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            try {
                final int mapId = Integer.parseInt(splitted[1]);

                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    if (cserv.getMapFactory().isMapLoaded(mapId) && cserv.getMapFactory().getMap(mapId).getCharactersSize() > 0) {
                        System.out.println("該地圖還有人唷");
                        return 0;
                    }
                }
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    if (cserv.getMapFactory().isMapLoaded(mapId)) {
                        cserv.getMapFactory().removeMap(mapId);
                    }
                }
            } catch (Exception e) {
                System.out.println("[指令用法] reloadMap <地圖ID>");
            }
            return 1;
        }
    }

    public static class help extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            System.out.println("╭〝☆指令列表〞★╮");
            System.out.println("-------------------------");
            System.out.println("exprate  經驗倍率");
            System.out.println("droprate 掉寶倍率");
            System.out.println("mesorate 金錢倍率");
            System.out.println("-------------------------");
            System.out.println("shutdown 關閉伺服器");
            System.out.println("shotdowntime <時間> 倒數關閉服務器");
            System.out.println("CrucialTime <時間> 關鍵時刻");
            System.out.println("reloadchannel 重新載入頻道");
            System.out.println("reloadmap 重新載入地圖");
            System.out.println("Info 查看伺服器狀況");
            System.out.println("AutoReg 自動註冊開關");
            System.out.println("-------------------------");
            System.out.println("online 線上玩家");
            System.out.println("say 伺服器說話");
            System.out.println("dodown 解除所有卡帳");
            System.out.println("saveall 全服存檔");
            System.out.println("GMmessage 後台GM廣播");
            System.out.println("-------------------------");
            System.out.println("╰〝★指令列表〞╯");
            return 1;
        }
    }

    public static class Online extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            int total = 0;
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                System.out.println("----------------------------------------------------------");
                System.out.println(new StringBuilder().append("頻道: ").append(ch.getChannel()).append(" 線上人數: ").append(ch.getConnectedClients()).toString());
                total += ch.getConnectedClients();
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharactersThreadSafe()) {

                    if (chr != null) {
                        StringBuilder ret = new StringBuilder();
                        ret.append(" 角色暱稱 ");
                        ret.append(StringUtil.getRightPaddedStr(chr.getName(), ' ', 13));
                        ret.append(" ID: ");
                        ret.append(StringUtil.getRightPaddedStr(chr.getId() + "", ' ', 4));
                        ret.append(" 等級: ");
                        ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getLevel()), ' ', 3));
                        ret.append(" 職業: ");
                        ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getJob()), ' ', 4));
                        if (chr.getMap() != null) {
                            ret.append(" 地圖: ");
                            ret.append(chr.getMapId()).append(" - ").append(chr.getMap().getMapName());
                            System.out.println(ret.toString());
                        }
                    }
                }
                System.out.println(new StringBuilder().append("當前頻道總計線上人數: ").append(total).toString());
                System.out.println("-------------------------------------------------------------------------------------");
            }

            System.out.println(new StringBuilder().append("當前伺服器總計線上人數: ").append(total).append("個").toString());
            System.out.println("-------------------------------------------------------------------------------------");

            return 1;
        }
    }

    public static class Say extends ConsoleCommandExecute {

        @Override
        public int execute(String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[伺服器公告] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
            } else {
                System.out.println("指令規則: say <message>");
                return 0;
            }
            return 1;
        }
    }

    public static class ReloadOps extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            SendPacketOpcode.reloadValues();
            RecvPacketOpcode.reloadValues();
            return 1;
        }

    }

    public static class ReloadDrops extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            return 1;
        }

    }

    public static class ReloadPortals extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            PortalScriptManager.getInstance().clearScripts();
            return 1;
        }
    }

    public static class ReloadShops extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            MapleShopFactory.getInstance().clear();
            return 1;
        }

    }

    public static class ReloadCS extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            CashItemFactory.getInstance().clearItems();
            return 1;
        }

    }

    public static class ReloadFishing extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            FishingRewardFactory.getInstance().reloadItems();
            return 1;
        }

    }

    public static class ReloadEvents extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
            return 1;
        }

    }

    public static class GMmessage extends ConsoleCommandExecute {

        @Override
        public int execute(String splitted[]) {
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[後台GM廣播] " + outputMessage));
            return 1;
        }

    }*/
}
