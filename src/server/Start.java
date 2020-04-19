package server;

import client.LoginCrypto;
import client.MapleCharacter;
import client.SkillFactory;
import client.inventory.OnlyID;
import constants.GameConstants;
import constants.PiPiConfig;
import constants.ServerConfig;
import constants.WorldConstants;
import database.DBConPool;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.login.LoginServer;
import handling.cashshop.CashShopServer;
import handling.login.LoginInformationProvider;
import handling.world.World;
import java.sql.SQLException;
import handling.world.family.MapleFamilyBuff;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import server.Timer.*;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.life.PlayerNPC;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import static tools.FileoutputUtil.CurrentReadable_Time;
import tools.MacAddressTool;
import tools.MaplePacketCreator;

public class Start {

    public static final Start instance = new Start();
    public static boolean Check = true;
    public static Map<String, Integer> ConfigValuesMap = new HashMap<>();
    public static int 初始通缉令 = 0;
    public static Boolean 每日送货 = false;

    private static void resetAllLoginState() {
        String name = null;
        int id = 0, vip = 0, size = 0;

        try (Connection con = DBConPool.getInstance().getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = 0")) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            FileoutputUtil.outError("logs/資料庫異常.txt", ex);
            throw new RuntimeException("【錯誤】 請確認資料庫是否正確連接");
        }
        /*try (Connection con = DBConPool.getInstance().getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement("SELECT count(*) FROM characters WHERE gm = 100"); ResultSet rs = ps.executeQuery()) {
            rs.beforeFirst();
            while (rs.next()) {
                size = rs.getInt(1);
            }
        } catch (SQLException ex) {
            FileoutputUtil.outError("logs/資料庫異常.txt", ex);
            throw new RuntimeException("【錯誤】 請確認資料庫是否正確連接");
        }
        if (size > 1) {
            System.out.println("警告：資料表內ＧＭ權限異常 ");
        }*/

 /*try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select id, name, vip FROM accounts where vip > 12"); ResultSet rs = ps.executeQuery()) {
                rs.beforeFirst();
                while (rs.next()) {
                    name = rs.getString("name");
                    vip = rs.getInt("vip");
                    id = rs.getInt("id");
                    System.err.println("VIP權限異常: 帳號[" + name + "], 編號[" + id + "], VIP[" + vip + "]");
                }
            }
        } catch (SQLException ex) {
            FileoutputUtil.outError("logs/資料庫異常.txt", ex);
            throw new RuntimeException("【錯誤】 請確認資料庫是否正確連接");
        }*/

 /*try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT inventoryequipmentid FROM inventoryequipment WHERE inventoryequipmentid >= 9000000000 ORDER BY inventoryequipmentid DESC LIMIT 1"); ResultSet rs = ps.executeQuery()) {
                rs.beforeFirst();
                while (rs.next()) {
                    throw new RuntimeException("資料表[inventoryequipment] 欄位[inventoryequipmentid] 流水號已達 : " + rs.getLong("inventoryequipmentid"));
                }
            }
        } catch (SQLException ex) {
            FileoutputUtil.outError("logs/資料庫異常.txt", ex);
            throw new RuntimeException("【錯誤】 請確認資料庫是否正確連接");
        }

        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT queststatusid FROM queststatus WHERE queststatusid >= 9000000000 ORDER BY queststatusid DESC LIMIT 1"); ResultSet rs = ps.executeQuery()) {
                rs.beforeFirst();
                while (rs.next()) {
                    throw new RuntimeException("資料表[queststatus] 欄位[queststatusid] 流水號已達 : " + rs.getLong("queststatusid"));
                }
            }
        } catch (SQLException ex) {
            FileoutputUtil.outError("logs/資料庫異常.txt", ex);
            throw new RuntimeException("【錯誤】 請確認資料庫是否正確連接");
        }*/
    }

    public void run() throws InterruptedException {
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        System.setProperty("file.encoding", "utf-8");
        System.setProperty("path", "");
        System.out.println("【冒险岛模擬器】");
        System.out.println("【版本】 v079");
        resetAllLoginState();

        if (WorldConstants.ADMIN_ONLY) {
            System.out.println("【管理員模式】開啟");
        } else {
            System.out.println("【管理員模式】關閉");
        }

        if (ServerConfig.AUTO_REGISTER) {
            System.out.println("【自動註冊】開啟");
        } else {
            System.out.println("【自動註冊】關閉");
        }

        if (!WorldConstants.GMITEMS) {
            System.out.println("【允許玩家使用管理員物品】開啟");
        } else {
            System.out.println("【允許玩家使用管理員物品】關閉");
        }

        /* 載入設定 */
        ServerConfig.loadSetting();
        World.init();
        /* 載入計時器 */
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        MobTimer.getInstance().start();
        CloneTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();
        /* 讀取WZ內禁止使用的名稱 */
        LoginInformationProvider.getInstance();
        /* 讀取釣魚 */
        FishingRewardFactory.getInstance();
        /* 載入任務*/
        MapleQuest.initQuests();
        MapleLifeFactory.loadQuestCounts();
        MapleOxQuizFactory.getInstance().initialize();
        /* 載入物品資訊 */
        MapleItemInformationProvider.getInstance().load();
        //MapleItemInformationProvider.loadFaceHair(); //载入脸型发型信息
        PredictCardFactory.getInstance().initialize();
        //MTSStorage.load();
        CashItemFactory.getInstance().initialize();
        /* 載入隨機獎勵 */
        RandomRewards.getInstance();
        /* 載入技能資訊 */
        SkillFactory.LoadSkillInformaion();
        /* 載入怪物技能 */
        MapleCarnivalFactory.getInstance();
        /* 載入排行 */
        MapleGuildRanking.getInstance().getGuildRank();
        MapleGuildRanking.getInstance().getJobRank(1);
        MapleGuildRanking.getInstance().getJobRank(2);
        MapleGuildRanking.getInstance().getJobRank(3);
        MapleGuildRanking.getInstance().getJobRank(4);
        MapleGuildRanking.getInstance().getJobRank(5);
        MapleGuildRanking.getInstance().getJobRank(6);
        /* 載入家族Buff */
        MapleFamilyBuff.getBuffEntry();
        /* 載入登入伺服器 */
        LoginServer.setup();
        /* 載入頻道伺服器*/
        ChannelServer.startAllChannels();
        /* 載入商城伺服器*/
        CashShopServer.setup();
        /* 載入自動封鎖系統 */
        CheatTimer.getInstance().register(AutobanManager.getInstance(), 60000);
        /* 載入關閉伺服器線程 */
        Runtime.getRuntime().addShutdownHook(new Thread(ShutdownServer.getInstance()));
        /* 載入速度排行 */
        SpeedRunner.getInstance().loadSpeedRuns();
        /* 處理怪物重生、CD、寵物、坐騎 */
        World.registerRespawn();
        /* 加載玩家NPC */
        PlayerNPC.loadAll();// touch - so we see database problems early...
        /* 設定finishedShutdown為false */
        LoginServer.setOn();
        /* 載入自訂義NPC、怪物*/
        MapleMapFactory.loadCustomLife();
        /* 載入自訂義功能 */
        公告(5);
        World.GainZx(1);
        World.GainCz(1);
        ZaiXian(1);
        //World.GainLx(3);
        //World.GainNX(60);// 每六十分鐘自動給點數
        //World.GainGash(60);
        //World.AutoSave(5);// 每五分鐘自動存檔
        //   World.ClearMemory(5 * 60);// 每小時清理記憶體
        //   WorldTimer.getInstance().register(CloseSQLConnections, 60 * 60 * 1000);// 定時清理MySql連接數
        World.isShutDown = false;
        OnlyID.getInstance();
        //System.out.println("【禁止玩家使用:啟動 如果要開放請GM上線打:!禁止玩家使用】");
        System.out.println("【伺服器開啟完畢】");
    }

    public static void main(final String args[]) throws InterruptedException {
        String[] macs = {
            "6d54e5dc940444564b3f595eea25561ae075183d",};
        String mac = MacAddressTool.getMacAddress(false);
        String num = returnSerialNumber();
        String localMac = LoginCrypto.hexSha1(num + mac);
        //System.out.println("" + "" + localMac);
        if (localMac != null) {
            for (int i = 0; i < macs.length; i++) {
                if (macs[i].equals(localMac)) {
                    instance.run();
                    break;
                }
            }
        } else {
            System.exit(0);
        }
    }

    public static String returnSerialNumber() {
        String cpu = getCPUSerial();
        String disk = getHardDiskSerialNumber("C");

        int newdisk = Integer.parseInt(disk);

        String s = cpu + newdisk;
        String newStr = s.substring(8, s.length());
        return newStr;
    }

    public static String getCPUSerial() {
        String result = "";
        try {
            File file = File.createTempFile("tmp", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new FileWriter(file);
            String vbs = "Set objWMIService = GetObject(\"winmgmts:\\\\.\\root\\cimv2\")\nSet colItems = objWMIService.ExecQuery _ \n   (\"Select * from Win32_Processor\") \nFor Each objItem in colItems \n    Wscript.Echo objItem.ProcessorId \n    exit for  ' do the first cpu only! \nNext \n";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = input.readLine()) != null) {
                result = result + line;
            }
            input.close();
            file.delete();
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        if ((result.trim().length() < 1) || (result == null)) {
            result = "无机器码被读取";
        }
        return result.trim();
    }

    public static String getHardDiskSerialNumber(String drive) {
        String result = "";
        try {
            File file = File.createTempFile("realhowto", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new FileWriter(file);
            String vbs = "Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\nSet colDrives = objFSO.Drives\nSet objDrive = colDrives.item(\"" + drive + "\")\n" + "Wscript.Echo objDrive.SerialNumber";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                result = result + line;
            }
            input.close();
        } catch (Exception e) {
        }
        return result.trim();
    }

    public static void 公告(final int time) {
        Timer.WorldTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                String 公告 = 本地取广播();
                if (!"".equals(公告)) {
                    World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat("[冒险岛 帮助] " + 公告));
                }
            }
        }, time * 1000 * 60);
    }

    private static int 本地取广播 = 0;

    public static String 本地取广播() {

        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM `广播信息` ORDER BY RAND() LIMIT 0,100;  ");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                data = rs.getString("广播");
            }
            if (本地取广播 > 0) {
                System.err.println("[服务端]" + CurrentReadable_Time() + " : 服务端输出循环广播 √");
            } else {
                本地取广播++;
            }
            ps.close();
        } catch (SQLException ex) {
            System.err.println("[服务端]" + CurrentReadable_Time() + " : 服务端输出循环广播 ×");
        }
        return data;
    }

    public static void 神秘商人(final int time) {
        Timer.WorldTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int 时 = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                int 分 = Calendar.getInstance().get(Calendar.MINUTE);
                int 星期 = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

                /**
                 * <启动神秘商人>
                 */
                if (ConfigValuesMap.get("神秘商人开关") == 0) {
                    //第一次启动神秘商人
                    if (活动神秘商人.神秘商人线程 == 0) {
                        活动神秘商人.启动神秘商人();
                        活动神秘商人.神秘商人线程++;
                    }
                    //召唤神秘商人
                    if (活动神秘商人.神秘商人线程 > 0) {
                        if (时 == 活动神秘商人.神秘商人时间 && 活动神秘商人.神秘商人 == 0) {
                            活动神秘商人.召唤神秘商人();
                        }
                    }

                }
            }
        }, 60 * 1000 * time
        );
    }

    public static void GetConfigValues() {
        //动态数据库连接
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT name, val FROM ConfigValues");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    int val = rs.getInt("val");
                    ConfigValuesMap.put(name, val);
                }
            }
            ps.close();
        } catch (SQLException ex) {
            System.err.println("读取动态数据库出错：" + ex.getMessage());
        }
    }

    public static void 野外通缉(final int time) {
        Timer.WorldTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int 时 = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                int 分 = Calendar.getInstance().get(Calendar.MINUTE);
                int 星期 = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

                /**
                 * <初始化通缉令>
                 */
                if (Start.ConfigValuesMap.get("野外通缉开关") == 0) {
                    if (初始通缉令 == 30) {
                        活动野外通缉.随机通缉();
                        初始通缉令 = 0;
                    } else {
                        初始通缉令++;
                    }
                }
            }
        }, 60 * 1000 * time
        );
    }

    public static void 每日送货(final int time) {
        Timer.WorldTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int 时 = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                int 分 = Calendar.getInstance().get(Calendar.MINUTE);
                int 星期 = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

                if (时 == 0 && 分 == 0) {
                    每日送货 = false;
                }
                /**
                 * <每日送货>
                 */
                if (Start.ConfigValuesMap.get("每日送货开关") == 0) {
                    if (时 == 12 && 每日送货 == false) {
                        for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                            for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                                mch.startMapEffect("[每日送货]: 送货活动开启，明珠港送货员处可以开始送货哦，可以获得丰厚的奖励。", 5120027);
                            }
                        }

                        每日送货 = true;
                    }
                }
            }
        }, 60 * 1000 * time
        );
    }

    public void ZaiXian(int time) {
        Timer.WorldTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {

                for (ChannelServer cs : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {

                        chr.gainTodayOnlineTime(1);
                        chr.gainTotalOnlineTime(1);

                    }
                }

            }
        }, 60 * 1000 * time
        );

    }

}
