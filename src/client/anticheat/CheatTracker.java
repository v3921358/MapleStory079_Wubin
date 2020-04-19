package client.anticheat;

import client.MapleBuffStat;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import constants.GameConstants;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.SkillFactory;
import constants.PiPiConfig;
import constants.WorldConstants;
import handling.world.World;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import server.AutobanManager;
import server.Timer.CheatTimer;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.StringUtil;

public class CheatTracker {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock rL = lock.readLock(), wL = lock.writeLock();
    private final Map<CheatingOffense, CheatingOffenseEntry> offenses = new LinkedHashMap<>();
    private final WeakReference<MapleCharacter> chr;
    // For keeping track of speed attack hack.
    private long lastAttackTime = 0;
    private int inMapIimeCount = 0;
    private int lastAttackTickCount = 0;
    private byte Attack_tickResetCount = 0;
    private long Server_ClientAtkTickDiff = 0;
    private long lastDamage = 0;
    private long takingDamageSince;
    private int numSequentialDamage = 0;
    private long lastDamageTakenTime = 0;
    private byte numZeroDamageTaken = 0;
    private int numSequentialSummonAttack = 0;
    private long summonSummonTime = 0;
    private int numSameDamage = 0;
    private Point lastMonsterMove;
    private int monsterMoveCount;
    private int attacksWithoutHit = 0;
    private byte dropsPerSecond = 0;
    private long lastDropTime = 0;
    private byte msgsPerSecond = 0;
    private long lastMsgTime = 0;
    private ScheduledFuture<?> invalidationTask;
    private int gm_message = 100;
    private int lastTickCount = 0, tickSame = 0;
    private long lastASmegaTime = 0;
    private long[] lastTime = new long[6];
    private long lastSaveTime = 0;
    private long lastLieDetectorTime = 0;
    private long lastLieTime = 0;

    public CheatTracker(final MapleCharacter chr) {
        this.chr = new WeakReference<>(chr);
        invalidationTask = CheatTimer.getInstance().register(new InvalidationTask(), 60000);
        takingDamageSince = System.currentTimeMillis();
    }

    public final void checkAttack(final int skillId, final int tickcount) {
        short AtkDelay = GameConstants.getAttackDelay(skillId);
        if (chr.get().getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null) {
            AtkDelay /= 6;// 使用這Buff之後 tickcount - lastAttackTickCount 可以為0...
        }
        // 攻擊加速
        if (chr.get().getBuffedValue(MapleBuffStat.BOOSTER) != null) {
            AtkDelay /= 1.5;
        }
        // 最終極速
        if (chr.get().getBuffedValue(MapleBuffStat.SPEED_INFUSION) != null) {
            AtkDelay /= 1.35;
        }
        // 狂郎
        if (GameConstants.isAran(chr.get().getJob())) {
            AtkDelay /= 1.4;// 407
        }
        // 海盜、拳霸
        if (chr.get().getJob() >= 500 && chr.get().getJob() <= 512) {
            AtkDelay = 0;// 407
        }
        // 強化連擊
        if (skillId == 21101003 || skillId == 5110001) {
            AtkDelay = 0;
        }
        if ((tickcount - lastAttackTickCount) < AtkDelay) {
            /*if (chr.get().get打怪() >= 100) {
                if (!chr.get().hasGmLevel(1)) {
                    chr.get().ban(chr.get().getName() + "攻擊速度異常，技能：" + skillId, true, true, false);
                    chr.get().getClient().getSession().close();
                    String reason = "使用違法程式練功";
                    World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + chr.get().getName() + " 因為" + reason + "而被管理員永久停權。"));
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + chr.get().getName() + " 攻擊無延遲自動封鎖! "));
                } else {
                    chr.get().dropMessage("觸發攻擊速度封鎖");
                }
                FileoutputUtil.logToFile("logs/Hack/Ban/攻擊速度.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.get().getName() + " 職業:" + chr.get().getJob() + " 技能: " + skillId + " check: " + (tickcount - lastAttackTickCount) + " " + "AtkDelay: " + AtkDelay);
                return;
            }*/
            if (GameConstants.getWuYanChi(skillId)) {
                FileoutputUtil.logToFile("logs/Hack/攻擊速度異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.get().getName() + " 職業:" + chr.get().getJob() + "　技能: " + skillId + "(" + SkillFactory.getSkillName(skillId) + ")" + " check: " + (tickcount - lastAttackTickCount) + " " + "AtkDelay: " + AtkDelay);

                //chr.get().add打怪();
                //registerOffense(CheatingOffense.FASTATTACK, "攻擊速度異常，技能: " + skillId + " check: " + (tickcount - lastAttackTickCount) + " " + "AtkDelay: " + AtkDelay);
                if (WorldConstants.WUYANCHI) {
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + " ID " + chr.get().getId() + " " + chr.get().getName() + " 攻擊速度異常，技能: " + skillId + "(" + SkillFactory.getSkillName(skillId) + ")"));
                }
            }
        }

        if (chr.get().getDebugMessage()) {
            chr.get().dropMessage("Delay [" + skillId + "] = " + (tickcount - lastAttackTickCount) + ", " + (AtkDelay));
        }

        if (WorldConstants.LieDetector) {
            this.lastAttackTime = System.currentTimeMillis();
            if ((this.chr.get() != null) && (this.lastAttackTime - ((MapleCharacter) this.chr.get()).getChangeTime() > 60000)) {
                ((MapleCharacter) this.chr.get()).setChangeTime(false);

                if ((!GameConstants.isBossMap(chr.get().getMapId())) && (((MapleCharacter) this.chr.get()).getEventInstance() == null) && (((MapleCharacter) this.chr.get()).getMap().getMobsSize() >= 1)) {
                    this.inMapIimeCount += 1;
                    if (this.inMapIimeCount >= 30) {
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + " ID " + chr.get().getId() + " " + chr.get().getName() + " 打怪時間超過 30 分鐘，該玩家可能在掛機。 "));
                    }
                    if (this.inMapIimeCount >= 30) {
                        this.inMapIimeCount = 0;
                        ((MapleCharacter) this.chr.get()).startLieDetector(false);
                        World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + " ID " + chr.get().getId() + " " + chr.get().getName() + " 打怪時間超過 30 分鐘，系統啟動測謊儀系統。 "));
                    }
                }
            }
        }
        final long STime_TC = System.currentTimeMillis() - tickcount; // hack = - more
        if (Server_ClientAtkTickDiff - STime_TC > 1000) { // 250 is the ping, TODO
            if (GameConstants.getWuYanChi(skillId)) {
                if (WorldConstants.WUYANCHI) {
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + " ID " + chr.get().getId() + " " + chr.get().getName() + " 攻擊速度異常，技能: " + skillId + "(" + SkillFactory.getSkillName(skillId) + ")"));
                }
                //registerOffense(CheatingOffense.FASTATTACK2, "攻擊速度異常，技能: " + skillId + " check: " + (tickcount - lastAttackTickCount) + " " + "AtkDelay: " + AtkDelay);
            }
        }

        Server_ClientAtkTickDiff = STime_TC;

//	System.out.println("Delay [" + skillId + "] = " + (tickcount - lastAttackTickCount) + ", " + (Server_ClientAtkTickDiff - STime_TC));
//
        chr.get().updateTick(tickcount);
        lastAttackTickCount = tickcount;
    }

    public final void checkTakeDamage(final int damage) {
        numSequentialDamage++;
        lastDamageTakenTime = System.currentTimeMillis();

        // System.out.println("tb" + timeBetweenDamage);
        // System.out.println("ns" + numSequentialDamage);
        // System.out.println(timeBetweenDamage / 1500 + "(" + timeBetweenDamage / numSequentialDamage + ")");
        if (lastDamageTakenTime - takingDamageSince / 500 < numSequentialDamage) {
//            registerOffense(CheatingOffense.FAST_TAKE_DAMAGE);
        }
        if (lastDamageTakenTime - takingDamageSince > 4500) {
            takingDamageSince = lastDamageTakenTime;
            numSequentialDamage = 0;
        }
        /*	(non-thieves)
         Min Miss Rate: 2%
         Max Miss Rate: 80%
         (thieves)
         Min Miss Rate: 5%
         Max Miss Rate: 95%*/
        if (damage == 0) {
            numZeroDamageTaken++;
            if (numZeroDamageTaken >= 35) { // Num count MSEA a/b players
                numZeroDamageTaken = 0;
                registerOffense(CheatingOffense.HIGH_AVOID, "迴避率過高 ");
            }
        } else if (damage != -1) {
            numZeroDamageTaken = 0;
        }
    }

    public final void checkSameDamage(final int dmg, final double expected) {
        if (dmg > 2000 && lastDamage == dmg && chr.get() != null && (chr.get().getLevel() < 175 || dmg > expected * 2)) {
            numSameDamage++;

            if (numSameDamage > 5) {
                numSameDamage = 0;
                registerOffense(CheatingOffense.SAME_DAMAGE, numSameDamage + " 次, 攻擊傷害: " + dmg + ", 預計傷害: " + expected + " [等級: " + chr.get().getLevel() + ", 職業: " + chr.get().getJob() + "]");
            }
        } else {
            lastDamage = dmg;
            numSameDamage = 0;
        }
    }

    public final void checkMoveMonster(final Point pos) {
        if (pos == lastMonsterMove) {
            monsterMoveCount++;
            if (monsterMoveCount > 10) {
                //registerOffense(CheatingOffense.MOVE_MONSTERS, "Position: " + pos.x + ", " + pos.y);
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + chr.get().getName() + " (編號: " + chr.get().getId() + ")使用吸怪(" + chr.get().get吸怪() + ")! - 地圖:" + chr.get().getMapId() + "(" + chr.get().getMap().getMapName() + ")"), true);
                monsterMoveCount = 0;
            }
        } else {
            lastMonsterMove = pos;
            monsterMoveCount = 1;
        }
    }

    public final void resetSummonAttack() {
        summonSummonTime = System.currentTimeMillis();
        numSequentialSummonAttack = 0;
    }

    public final boolean checkSummonAttack() {
        numSequentialSummonAttack++;
        //estimated
        // System.out.println(numMPRegens + "/" + allowedRegens);
        // long time = (System.currentTimeMillis() - summonSummonTime) / (2000 + 1) + 3l;
        //  if (time < numSequentialSummonAttack) {
        //        registerOffense(CheatingOffense.FAST_SUMMON_ATTACK, chr.get().getName() + "快速召喚獸攻擊 " + time + " < " + numSequentialSummonAttack);
        //      return false;
        //  }
        return true;
    }

    public final void checkDrop() {
        checkDrop(false);
    }

    public final void checkDrop(final boolean dc) {
        if ((System.currentTimeMillis() - lastDropTime) < 1000) {
            dropsPerSecond++;
            if (dropsPerSecond >= (dc ? 32 : 16) && chr.get() != null && !chr.get().isGM()) {
                if (dc) {
                    chr.get().getClient().getSession().close();
                } else {
                    chr.get().getClient().setMonitored(true);
                }
            }
        } else {
            dropsPerSecond = 0;
        }
        lastDropTime = System.currentTimeMillis();
    }

    public boolean canAvatarSmega2() {
        long time = 10 * 1000;
        if (chr.get() != null) {
            if (chr.get().getId() == 845 || chr.get().getId() == 5247 || chr.get().getId() == 12048) {
                time = 20 * 1000;
            }
            if (lastASmegaTime + time > System.currentTimeMillis() && !chr.get().isGM()) {
                return false;
            }
        }
        lastASmegaTime = System.currentTimeMillis();
        return true;
    }

    public synchronized boolean GMSpam(int limit, int type) {
        if (type < 0 || lastTime.length < type) {
            type = 1; // default xD
        }
        if (System.currentTimeMillis() < limit + lastTime[type]) {
            return true;
        }
        lastTime[type] = System.currentTimeMillis();
        return false;
    }

    public final void checkMsg() { //ALL types of msg. caution with number of  msgsPerSecond
        if ((System.currentTimeMillis() - lastMsgTime) < 1000) { //luckily maplestory has auto-check for too much msging
            msgsPerSecond++;
            /*            if (msgsPerSecond > 10 && chr.get() != null) {
             chr.get().getClient().getSession().close();
             }*/
        } else {
            msgsPerSecond = 0;
        }
        lastMsgTime = System.currentTimeMillis();
    }

    public final int getAttacksWithoutHit() {
        return attacksWithoutHit;
    }

    public final void setAttacksWithoutHit(final boolean increase) {
        if (increase) {
            this.attacksWithoutHit++;
        } else {
            this.attacksWithoutHit = 0;
        }
    }

    public final void registerOffense(final CheatingOffense offense) {
        registerOffense(offense, null);
    }

    public final void registerOffense(final CheatingOffense offense, final String param) {
        final MapleCharacter chrhardref = chr.get();
        if (chrhardref == null || !offense.isEnabled() || chrhardref.isClone()) {
            return;
        }
        if (chr.get().hasGmLevel(5)) {
            chr.get().dropMessage("註冊：" + offense + " 原因：" + param);
        }
        CheatingOffenseEntry entry = null;
        rL.lock();
        try {
            entry = offenses.get(offense);
        } finally {
            rL.unlock();
        }
        if (entry != null && entry.isExpired()) {
            expireEntry(entry);
            entry = null;
        }
        if (entry == null) {
            entry = new CheatingOffenseEntry(offense, chrhardref.getId());
        }
        if (param != null) {
            entry.setParam(param);
        }
        entry.incrementCount();
        if (offense.shouldAutoban(entry.getCount())) {
            final byte type = offense.getBanType();
            String outputFileName;
            if (type == 1) {
                AutobanManager.getInstance().autoban(chrhardref.getClient(), StringUtil.makeEnumHumanReadable(offense.name()));
            } else if (type == 2) {
                outputFileName = "斷線";
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + chrhardref.getName() + " 自動斷線 類別: " + offense.toString() + " 原因: " + (param == null ? "" : (" - " + param))));
                FileoutputUtil.logToFile("logs/Hack/" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.get().getName() + " 項目：" + offense.toString() + " 原因： " + (param == null ? "" : (" - " + param)));
                chrhardref.getClient().getSession().close();
            } else if (type == 3) {
                boolean ban = true;
                outputFileName = "封鎖";
                String show = "使用違法程式練功";
                String real = "";
                if (offense.toString() == "ITEMVAC_SERVER") {
                    outputFileName = "全圖吸物";
                    real = "使用全圖吸物";
                    if (!PiPiConfig.getAutoban()) {
                        ban = false;
                    }
                } else if (offense.toString() == "FAST_SUMMON_ATTACK") {
                    outputFileName = "召喚獸無延遲";
                    real = "使用召喚獸無延遲攻擊";
                } else if (offense.toString() == "MOB_VAC") {
                    outputFileName = "吸怪";
                    real = "使用吸怪";
                    if (!PiPiConfig.getAutoban()) {
                        ban = false;
                    }
                } else if (offense.toString() == "ATTACK_FARAWAY_MONSTER_BAN") {
                    outputFileName = "全圖打";
                    real = "使用全圖打";
                    if (!PiPiConfig.getAutoban()) {
                        ban = false;
                    }
                } else {
                    ban = false;
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + MapleCharacterUtil.makeMapleReadable(chrhardref.getName()) + " (編號: " + chrhardref.getId() + " )使用外掛! " + StringUtil.makeEnumHumanReadable(offense.name()) + (param == null ? "" : (" - " + param))));
                }

                if (chr.get().hasGmLevel(1)) {
                    chr.get().dropMessage("觸發違規: " + real + " param: " + (param == null ? "" : (" - " + param)));
                } else if (ban) {
                    FileoutputUtil.logToFile("logs/Hack/Ban/" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.get().getName() + " 項目：" + offense.toString() + " 原因： " + (param == null ? "" : (" - " + param)));
                    //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + chrhardref.getName() + " 因為" + show + "而被管理員永久停權。"));
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + chrhardref.getName() + " " + real + "自動封鎖! "));
                    //chrhardref.ban(chrhardref.getName() + real, true, true, false);
                    //chrhardref.getClient().getSession().close();
                } else {
                    FileoutputUtil.logToFile("logs/Hack/" + outputFileName + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家：" + chr.get().getName() + " 項目：" + offense.toString() + " 原因： " + (param == null ? "" : (" - " + param)));
                }
            }
            gm_message = 100;
            return;
        }

        wL.lock();

        try {
            offenses.put(offense, entry);
        } finally {
            wL.unlock();
        }
        switch (offense) {
            case FAST_SUMMON_ATTACK:
            case ITEMVAC_SERVER:
            case MOB_VAC:
            case HIGH_DAMAGE_MAGIC:
            case HIGH_DAMAGE_MAGIC_2:
            case HIGH_DAMAGE:
            case HIGH_DAMAGE_2:
            case ATTACK_FARAWAY_MONSTER:
            //case ATTACK_FARAWAY_MONSTER_SUMMON:
            case SAME_DAMAGE:
                gm_message--;
                boolean log = false;
                String out_log = "";
                String show = offense.name();
                switch (show) {
                    case "ATTACK_FARAWAY_MONSTER":
                        show = "全圖打";
                        out_log = "攻擊範圍異常";
                        log = true;
                        break;
                    case "MOB_VAC":
                        show = "使用吸怪";
                        out_log = "吸怪";
                        log = true;
                        break;
                }
                if (gm_message % 5 == 0) {
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM密語] " + chrhardref.getName() + " (編號:" + chrhardref.getId() + ")疑似外掛! " + show + (param == null ? "" : (" - " + param))));
                    if (log) {
                        FileoutputUtil.logToFile("logs/Hack/" + out_log + ".txt", "\r\n" + FileoutputUtil.NowTime() + " " + chrhardref.getName() + " (編號:" + chrhardref.getId() + ")疑似外掛! " + show + (param == null ? "" : (" - " + param)));
                    }
                }
                if (gm_message == 0) {
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[封號系統] " + chrhardref.getName() + " (編號: " + chrhardref.getId() + " )疑似外掛！" + show + (param == null ? "" : (" - " + param))));
                    AutobanManager.getInstance().autoban(chrhardref.getClient(), StringUtil.makeEnumHumanReadable(offense.name()));
                    gm_message = 100;
                }
                break;
        }
        CheatingOffensePersister.getInstance().persistEntry(entry);
    }

    public void updateTick(int newTick) {
        if (newTick == lastTickCount) { //definitely packet spamming
/*	    if (tickSame >= 5) {
             chr.get().getClient().getSession().close(); //i could also add a check for less than, but i'm not too worried at the moment :)
             } else {*/
            tickSame++;
//	    }
        } else {
            tickSame = 0;
        }
        lastTickCount = newTick;
    }

    public final void expireEntry(final CheatingOffenseEntry coe) {
        wL.lock();
        try {
            offenses.remove(coe.getOffense());
        } finally {
            wL.unlock();
        }
    }

    public final int getPoints() {
        int ret = 0;
        CheatingOffenseEntry[] offenses_copy;
        rL.lock();
        try {
            offenses_copy = offenses.values().toArray(new CheatingOffenseEntry[offenses.size()]);
        } finally {
            rL.unlock();
        }
        for (final CheatingOffenseEntry entry : offenses_copy) {
            if (entry.isExpired()) {
                expireEntry(entry);
            } else {
                ret += entry.getPoints();
            }
        }
        return ret;
    }

    public final Map<CheatingOffense, CheatingOffenseEntry> getOffenses() {
        return Collections.unmodifiableMap(offenses);
    }

    public final String getSummary() {
        final StringBuilder ret = new StringBuilder();
        final List<CheatingOffenseEntry> offenseList = new ArrayList<>();
        rL.lock();
        try {
            for (final CheatingOffenseEntry entry : offenses.values()) {
                if (!entry.isExpired()) {
                    offenseList.add(entry);
                }
            }
        } finally {
            rL.unlock();
        }
        Collections.sort(offenseList, new Comparator<CheatingOffenseEntry>() {

            @Override
            public final int compare(final CheatingOffenseEntry o1, final CheatingOffenseEntry o2) {
                final int thisVal = o1.getPoints();
                final int anotherVal = o2.getPoints();
                return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
            }
        });
        final int to = Math.min(offenseList.size(), 4);
        for (int x = 0; x < to; x++) {
            ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).getOffense().name()));
            ret.append(": ");
            ret.append(offenseList.get(x).getCount());
            if (x != to - 1) {
                ret.append(" ");
            }
        }
        return ret.toString();
    }

    public final void dispose() {
        if (invalidationTask != null) {
            invalidationTask.cancel(false);
        }
        invalidationTask = null;

    }

    private final class InvalidationTask implements Runnable {

        @Override
        public final void run() {
            CheatingOffenseEntry[] offenses_copy;
            rL.lock();
            try {
                offenses_copy = offenses.values().toArray(new CheatingOffenseEntry[offenses.size()]);
            } finally {
                rL.unlock();
            }
            for (CheatingOffenseEntry offense : offenses_copy) {
                if (offense.isExpired()) {
                    expireEntry(offense);
                }
            }
            if (chr.get() == null) {
                dispose();
            }
        }
    }

    public boolean canSaveDB() {
        if ((System.currentTimeMillis() - lastSaveTime < 5 * 60 * 1000)) {
            return false;
        }
        this.lastSaveTime = System.currentTimeMillis();
        return true;
    }

    public int getlastSaveTime() {
        return (int) ((System.currentTimeMillis() - this.lastSaveTime) / 1000);
    }

    public int getlastLieTime() {
        return (int) ((System.currentTimeMillis() - this.lastLieTime) / 1000);
    }

    public boolean canLieDetector() {
        if ((this.lastLieDetectorTime + 300000 > System.currentTimeMillis()) && (this.chr.get() != null)) {
            return false;
        }
        this.lastLieDetectorTime = System.currentTimeMillis();
        return true;
    }

    public void resetInMapIimeCount() {
        this.inMapIimeCount = 0;
    }
}
