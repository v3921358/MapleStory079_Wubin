package server;

import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.util.Set;
import server.Timer.*;

public class ShutdownServer implements Runnable, ShutdownServerMBean {

    private static final ShutdownServer instance = new ShutdownServer();
    public static boolean running = false;

    public static ShutdownServer getInstance() {
        return instance;
    }

    @Override
    public void run() {
        synchronized (this) {
            if (running) { //Run once!
                return;
            }
            running = true;
        }
        World.isShutDown = true;
        int ret = 0;
        for (handling.channel.ChannelServer cserv : handling.channel.ChannelServer.getAllInstances()) {
            ret += cserv.closeAllMerchant();
        }
        System.out.println("共儲存了 " + ret + " 個精靈商人");
        ret = 0;
        for (handling.channel.ChannelServer cserv : handling.channel.ChannelServer.getAllInstances()) {
            ret += cserv.closeAllPlayerShop();
        }
        System.out.println("共儲存了 " + ret + " 個個人執照商店");
        World.Guild.save();
        System.out.println("公會資料儲存完畢");
        World.Alliance.save();
        System.out.println("聯盟資料儲存完畢");
        World.Family.save();
        System.out.println("家族資料儲存完畢");
        EventTimer.getInstance().stop();
        WorldTimer.getInstance().stop();
        MapTimer.getInstance().stop();
        MobTimer.getInstance().stop();
        BuffTimer.getInstance().stop();
        CloneTimer.getInstance().stop();
        EtcTimer.getInstance().stop();
        PingTimer.getInstance().stop();
        System.out.println("Timer 關閉完成");
        
        
        Set<Integer> channels = ChannelServer.getAllChannels();

        for (Integer channel : channels) {
            try {
                ChannelServer cs = ChannelServer.getInstance(channel);
                cs.saveAll();
                cs.setPrepareShutdown();
                cs.shutdown();
            } catch (Exception e) {
                System.out.println("頻道" + String.valueOf(channel) + " 關閉失敗.");
            }
        }

        try {
            LoginServer.shutdown();
            System.out.println("登陸伺服器關閉完成.");
        } catch (Exception e) {
            System.out.println("登陸伺服器關閉失敗");
        }
        try {
            CashShopServer.shutdown();
            System.out.println("購物商城伺服器關閉完成.");
        } catch (Exception e) {
            System.out.println("購物商城伺服器關閉失敗");
        }
//        try {
//            Thread.sleep(5000);
//        } catch (Exception e) {
//
//        }
//        System.exit(0);
    }

    @Override
    public void shutdown() {
        this.run();
    }
}
