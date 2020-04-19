package client;

import static client.MapleJob.values;
import constants.SkillConstants;

public enum MapleJob {

    新手(0),
    战士(100),
    剑客(110),
    勇士(111),
    英雄(112),
    准骑士(120),
    骑士(121),
    圣骑士(122),
    枪战士(130),
    龙骑士(131),
    黑骑士(132),
    魔法师(200),
    火毒法师(210),
    火毒巫师(211),
    火毒魔导师(212),
    冰雷法师(220),
    冰雷巫师(221),
    冰雷魔导师(222),
    牧师(230),
    祭司(231),
    主教(232),
    弓箭手(300),
    猎人(310),
    射手(311),
    神射手(312),
    弩弓手(320),
    游侠(321),
    箭神(322),
    飞侠(400),
    刺客(410),
    无影人(411),
    隐士(412),
    侠客(420),
    独行侠(421),
    侠盗(422),
    见习刀客(430),
    双刀客(431),
    双刀侠(432),
    血刀(433),
    暗影双刀(434),
    海盜(500),
    拳手(510),
    斗士(511),
    冲锋队长(512),
    火枪手(520),
    大副(521),
    船长(522),
    MANAGER(800),
    管理員(900),
    初心者(1000),
    魂骑士1转(1100),
    魂骑士2转(1110),
    魂骑士3转(1111),
    魂骑士4转(1112),
    炎术士1转(1200),
    炎术士2转(1210),
    炎术士3转(1211),
    炎术士4转(1212),
    风灵使者1转(1300),
    风灵使者2转(1310),
    风灵使者3转(1311),
    风灵使者4转(1312),
    夜行者1转(1400),
    夜行者2转(1410),
    夜行者3转(1411),
    夜行者4转(1412),
    奇袭者1转(1500),
    奇袭者2转(1510),
    奇袭者3转(1511),
    奇袭者4转(1512),
    战童(2000),
    龙神(2001),
    战神1转(2100),
    战神2转(2110),
    战神3转(2111),
    战神4转(2112),
    龙神1转(2200),
    龙神2转(2210),
    龙神3转(2211),
    龙神4转(2212),
    龙神5转(2213),
    龙神6转(2214),
    龙神7转(2215),
    龙神8转(2216),
    龙神9转(2217),
    龙神10转(2218),
    未知(999999),;
    private final int jobid;

    private MapleJob(int id) {
        this.jobid = id;
    }

    public int getId() {
        return this.jobid;
    }

    public static String getName(MapleJob mjob) {
        return mjob.name();
    }

    public static MapleJob getById(int id) {
        for (MapleJob l : values()) {
            if (l.getId() == id) {
                return l;
            }
        }
        return 未知;
    }

    public static boolean isExist(int id) {
        for (MapleJob job : values()) {
            if (job.getId() == id) {
                return true;
            }
        }
        return false;
    }

    public static boolean is冒險家(final int job) {
        return job / 1000 == 0;
    }

    public static boolean is英雄(final int job) {
        return job / 10 == 11;
    }

    public static boolean is聖騎士(final int job) {
        return job / 10 == 12;
    }

    public static boolean is黑騎士(final int job) {
        return job / 10 == 13;
    }

    public static boolean is大魔導士_火毒(final int job) {
        return job / 10 == 21;
    }

    public static boolean is大魔導士_冰雷(final int job) {
        return job / 10 == 22;
    }

    public static boolean is主教(final int job) {
        return job / 10 == 23;
    }

    public static boolean is箭神(final int job) {
        return job / 10 == 31;
    }

    public static boolean is神射手(final int job) {
        return job / 10 == 32;
    }

    public static boolean is夜使者(final int job) {
        return job / 10 == 41;
    }

    public static boolean is暗影神偷(final int job) {
        return job / 10 == 42;
    }

    public static boolean is影武者(final int job) {
        return job / 10 == 43; // sub == 1 && job == 400
    }

    public static boolean is拳霸(final int job) {
        return job / 10 == 51;
    }

    public static boolean is槍神(final int job) {
        return job / 10 == 52;
    }

    public static boolean is管理員(final int job) {
        return job == 800 || job == 900 || job == 910;
    }

    public static boolean is皇家騎士團(final int job) {
        return job / 1000 == 1;
    }

    public static boolean is聖魂劍士(final int job) {
        return job / 100 == 11;
    }

    public static boolean is烈焰巫師(final int job) {
        return job / 100 == 12;
    }

    public static boolean is破風使者(final int job) {
        return job / 100 == 13;
    }

    public static boolean is暗夜行者(final int job) {
        return job / 100 == 14;
    }

    public static boolean is閃雷悍將(final int job) {
        return job / 100 == 15;
    }

    public static boolean is英雄團(final int job) {
        return job / 1000 == 2;
    }

    public static boolean is狂狼勇士(final int job) {
        return job / 100 == 21 || job == 2000;
    }

    public static boolean is劍士(final int job) {
        return getJobBranch(job) == 1;
    }

    public static boolean is法師(final int job) {
        return getJobBranch(job) == 2;
    }

    public static boolean is弓箭手(final int job) {
        return getJobBranch(job) == 3;
    }

    public static boolean is盜賊(final int job) {
        return getJobBranch(job) == 4 || getJobBranch(job) == 6;
    }

    public static boolean is海盜(final int job) {
        return getJobBranch(job) == 5 || getJobBranch(job) == 6;
    }

    public static short getBeginner(final short job) {
        if (job % 1000 < 10) {
            return job;
        }
        switch (job / 100) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 8:
            case 9:
                return (short) 新手.getId();
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                return (short) 初心者.getId();
            case 20:
                return (short) 战童.getId();
            case 21:
                return (short) 战童.getId();
        }
        return (short) 新手.getId();
    }

    public static boolean is初心者(int jobid) {
        if (jobid <= 5000) {
            if (jobid != 5000 && (jobid < 2001 || jobid > 2005 && (jobid <= 3000 || jobid > 3002 && (jobid <= 4000 || jobid > 4002)))) {
            } else {
                return true;
            }
        } else if (jobid >= 6000 && (jobid <= 6001 || jobid == 13000)) {
            return true;
        }
        boolean result = isJob12000(jobid);
        if (jobid % 1000 == 0 || jobid / 100 == 8000 || jobid == 8001 || result) {
            result = true;
        }
        return result;
    }

    public static boolean isJob12000(int job) {
        boolean result = isJob12000HighLv(job);
        if (isJob12000LowLv(job) || result) {
            result = true;
        }
        return result;
    }

    public static boolean isJob12000HighLv(int job) {
        return job == 12003 || job == 12004;
    }

    public static boolean isJob12000LowLv(int job) {
        return job == 12000 || job == 12001 || job == 12002;
    }

    public static boolean isJob8000(int job) {
        int v1 = SkillConstants.getJobBySkill(job);
        return v1 >= 800000 && v1 <= 800099 || v1 == 8001;
    }

    public static boolean isJob9500(int job) {
        boolean result;
        if (job >= 0) {
            result = SkillConstants.getJobBySkill(job) == 9500;
        } else {
            result = false;
        }
        return result;
    }

    public static int get轉數(int jobid) {
        int result;
        if (is初心者(jobid) || jobid % 100 == 0 || jobid == 501 || jobid == 3101 || jobid == 508) {
            result = 1;
        } else {
            int v1 = jobid % 10;
            int v2;
            if (jobid / 10 == 43) {
                v2 = v1 / 2 + 2;
            } else {
                v2 = v1 + 2;
            }
          //  if (v2 >= 2 && (v2 <= 4 || v2 <= 10 && is龍魔導士(jobid))) {
            //       result = v2;
            //  } else {
            result = 0;
            //  }
        }
        return result;
    }

    public static boolean isBeginner(final int job) {
        return getJobGrade(job) == 0;
    }

    public static boolean isSameJob(int job, int job2) {
        int jobNum = getJobGrade(job);
        int job2Num = getJobGrade(job2);
        // 對初心者判斷
        if (jobNum == 0 || job2Num == 0) {
            return getBeginner((short) job) == getBeginner((short) job2);
        }

        // 初心者過濾掉后, 對職業群進行判斷
        if (getJobGroup(job) != getJobGroup(job2)) {
            return false;
        }

        // 代碼特殊的單獨判斷
        if (MapleJob.is管理員(job) || MapleJob.is管理員(job)) {
            return MapleJob.is管理員(job2) && MapleJob.is管理員(job2);
        }
//        } else if (MapleJob.is重砲指揮官(job) || MapleJob.is重砲指揮官(job)) {
//            return MapleJob.is重砲指揮官(job2) && MapleJob.is重砲指揮官(job2);
//        } else if (MapleJob.is蒼龍俠客(job) || MapleJob.is蒼龍俠客(job)) {
//            return MapleJob.is蒼龍俠客(job2) && MapleJob.is蒼龍俠客(job2);
//        } else if (MapleJob.is惡魔復仇者(job) || MapleJob.is惡魔復仇者(job)) {
//            return MapleJob.is惡魔復仇者(job2) && MapleJob.is惡魔復仇者(job2);
//        }

        // 對一轉分支判斷(如 劍士 跟 黑騎)
        if (jobNum == 1 || job2Num == 1) {
            return job / 100 == job2 / 100;
        }

        return job / 10 == job2 / 10;
    }

    public static int getJobGroup(int job) {
        return job / 1000;
    }

    public static int getJobBranch(int job) {
        if (job / 100 == 27) {
            return 2;
        } else {
            return job % 1000 / 100;
        }
    }

    public static int getJobBranch2nd(int job) {
        if (job / 100 == 27) {
            return 2;
        } else {
            return job % 1000 / 100;
        }
    }

    public static int getJobGrade(int jobz) {
        int job = (jobz % 1000);
        if (job / 10 == 0) {
            return 0; //beginner
        } else if (job / 10 % 10 == 0) {
            return 1;
        } else {
            return job % 10 + 2;
        }
    }
}
