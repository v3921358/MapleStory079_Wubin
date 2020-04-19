/*
 This file is part of the ZeroFusion MapleStory Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>
 ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

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
package server.events;

import java.util.concurrent.ScheduledFuture;
import client.MapleCharacter;
import server.Timer.EventTimer;
import tools.MaplePacketCreator;

public class MapleFitness extends MapleEvent {

    private static final long serialVersionUID = 845748950824L;
    private final long time = 600000; //change
    private long timeStarted = 0;
    private ScheduledFuture<?> fitnessSchedule, msgSchedule;

    public MapleFitness(final int channel, final int[] mapid) {
        super(channel, mapid);
    }

    @Override
    public void finished(final MapleCharacter chr) {
        givePrize(chr);
        //chr.finishAchievement(20);
    }

    @Override
    public void onMapLoad(MapleCharacter chr) {
        if (isTimerStarted()) {
            chr.getClient().sendPacket(MaplePacketCreator.getClock((int) (getTimeLeft() / 1000)));
        }
    }

    @Override
    public void startEvent() {
        unreset();
        super.reset(); //isRunning = true
        broadcast(MaplePacketCreator.getClock((int) (time / 1000)));
        this.timeStarted = System.currentTimeMillis();
        checkAndMessage();

        fitnessSchedule = EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < mapid.length; i++) {
                    for (MapleCharacter chr : getMap(i).getCharactersThreadsafe()) {
                        warpBack(chr);
                    }
                }
                unreset();
            }
        }, this.time);

        broadcast(MaplePacketCreator.serverNotice(0, "活動已經開始，請通過中間的入口開始遊戲。"));
    }

    public boolean isTimerStarted() {
        return timeStarted > 0;
    }

    public long getTime() {
        return time;
    }

    public void resetSchedule() {
        this.timeStarted = 0;
        if (fitnessSchedule != null) {
            fitnessSchedule.cancel(false);
        }
        fitnessSchedule = null;
        if (msgSchedule != null) {
            msgSchedule.cancel(false);
        }
        msgSchedule = null;
    }

    @Override
    public void reset() {
        super.reset();
        resetSchedule();
        getMap(0).getPortal("join00").setPortalState(false);
        getMap(0).getPortal("in00").setPortalState(false);
    }

    @Override
    public void unreset() {
        super.unreset();
        resetSchedule();
        getMap(0).getPortal("join00").setPortalState(true);
        getMap(0).getPortal("in00").setPortalState(true);
    }

    public long getTimeLeft() {
        return time - (System.currentTimeMillis() - timeStarted);
    }

    public void checkAndMessage() {
        msgSchedule = EventTimer.getInstance().register(new Runnable() {

            @Override
            public void run() {
                final long timeLeft = getTimeLeft();
                if (timeLeft > 9000 && timeLeft < 11000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "你還有10秒左右的時間，那些你不能擊敗的玩家，我希望你下次贏得勝利，回頭見。"));
                } else if (timeLeft > 11000 && timeLeft < 101000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "好吧，你剩下沒有多少時間了，請抓緊時間衝向終點。"));
                } else if (timeLeft > 101000 && timeLeft < 241000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "這已經是最後了不要放棄，豐富的大獎等著你！"));
                } else if (timeLeft > 241000 && timeLeft < 301000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "這跳完就剩下一階了加油！"));
                } else if (timeLeft > 301000 && timeLeft < 361000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "請小心掉落。"));
                } else if (timeLeft > 361000 && timeLeft < 501000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "請小心HP歸零。"));
                } else if (timeLeft > 501000 && timeLeft < 601000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "請小心猴子。"));
                } else if (timeLeft > 601000 && timeLeft < 661000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "第二階的技巧請利用猴子。"));
                } else if (timeLeft > 661000 && timeLeft < 701000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "請小心HP歸零。"));
                } else if (timeLeft > 701000 && timeLeft < 781000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "大家知道 [終極忍耐] 很好玩的！"));
                } else if (timeLeft > 781000 && timeLeft < 841000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "有可能會小LAG一下不過不需要擔心。"));
                } else if (timeLeft > 841000) {
                    broadcast(MaplePacketCreator.serverNotice(0, "[終極忍耐] 總共有四階，如果你碰巧在遊戲過程中死亡，你會從遊戲中消失，所以請注意這一點。"));
                }
            }
        }, 90000);
    }
}
