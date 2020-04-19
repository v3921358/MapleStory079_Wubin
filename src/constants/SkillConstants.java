/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import client.MapleJob;

/**
 *
 * @author pungin
 */
public class SkillConstants {

    public static boolean isSkill92XX0000(int skillId) {
        return skillId / 1000000 == 92 && skillId % 10000 == 0;
    }

    public static boolean isSkill92XX____(int skillId) {
        return !isSkill92XX0000(skillId) && isSkill92XX0000(10000 * (skillId / 10000));
    }

    public static boolean is4thNotNeedMasterLevel(int skillId) {
        if (skillId > 5220014) { // 雙倍幸運骰子
            if (skillId > 23120011) { // 旋風月光翻轉
                // 進階光速雙擊 || 勇士的意志 || 狂暴天性 || 雙倍幸運骰子
                if (skillId == 23120013 || skillId == 23121008 || skillId == 33120010 || skillId == 35120014) {
                    return true;
                }
                // 戰鬥大師
                return skillId == 51120000;
            } else {
                // 旋風月光翻轉
                if (skillId == 23120011
                        || skillId == 5320007 // 雙倍幸運骰子
                        || skillId == 5321004 // 雙胞胎猴子
                        || skillId == 5321006 // 楓葉淨化
                        || skillId == 21120011 // 快速移動
                        || skillId == 21120014) { // 屠魔勇氣
                    return true;
                }
                // 歐尼斯的意志
                return skillId == 22181004;
            }
        }
        if (skillId == 5220014) { // 雙倍幸運骰子
            return true;
        }
        if (skillId <= 4110012) { // 鏢術精通
            // 鏢術精通 || 戰鬥精通 || 靈魂復仇 || Null(沒找到技能) || Null(沒找到技能) || Null(沒找到技能)
            if (skillId == 4110012 || skillId == 1120012 || skillId == 1320011 || skillId == 2121009 || skillId == 2221009 || skillId == 2321010) {
                return true;
            }
            // 射擊術
            return skillId == 3210015;
        }
        // !貪婪 && !疾速 && !致命的飛毒殺
        if (skillId != 4210012 && skillId != 4340010 && skillId != 4340012) {
            // Null(沒找到技能)
            if (skillId <= 5120010) {
                return false;
            }
            // 雙倍幸運骰子 
            if (skillId > 5120012) {
                // 反擊
                return skillId == 5220012;
            }
        }
        return true;
    }

    public static boolean isNot4thNeedMasterLevel(int skillId) {
        if (skillId > 101100101) { // 進階武器投擲
            // 進階迴旋之刃 || 進階旋風 || 進階旋風急轉彎 || 進階旋風落葉斬 || 進階碎地猛擊
            if (skillId == 101100201 || skillId == 101110102 || skillId == 101110200 || skillId == 101110203 || skillId == 101120104) {
                return true;
            }
            return skillId == 101120204; // 進階暴風裂擊
        } else {
            if (skillId == 101100101 // 進階武器投擲
                    || skillId == 4311003 // 狂刃風暴
                    || skillId == 4321006 // 翔空落葉斬
                    || skillId == 4330009 // 暗影迴避
                    || skillId == 4331002 // 替身術
                    || skillId == 4341004 // 短刀護佑
                    || skillId == 4341007) { // 荊棘特效
                return true;
            }
            return skillId == 101000101; // 進階威力震擊
        }
    }

    public static boolean isSkillNeedMasterLevel(int skillId) {
        if (is4thNotNeedMasterLevel(skillId)) {
            return false;
        }
        if (isSkill92XX0000(skillId)) {
            return false;
        }
        if (isSkill92XX____(skillId)) {
            return false;
        }
        if (MapleJob.isJob8000(skillId)) {
            return false;
        }
        int jobid = getJobBySkill(skillId);
        if (MapleJob.is初心者(jobid) || MapleJob.isJob9500(skillId) || skillId == 42120024/*影朋‧花狐*/) {// || MapleJob.is幻獸師(jobid)
            return false;
        }
        int jobTimes = MapleJob.get轉數(jobid);
//        if (MapleJob.is龍魔導士(jobid)) {
//            if (skillId == 22171004) { // 楓葉淨化
//                return false;
//            }
//            if (jobTimes == 9) {
//                return true;
//            }
//            return jobTimes == 10;
//        } else {
        if (isNot4thNeedMasterLevel(skillId)) {
            return true;
        }
        if (jobTimes != 4) {
            return false;
        }
        return true;//!MapleJob.is神之子(jobid)
        //    }
    }

    public static int get紫扇傳授UnknownValue(int skillId) {
        int result;
        if (skillId == 40020002 || skillId == 80000004) {
            result = 100;
        } else {
            result = 0;
        }
        return result;
    }

    public static int getJobBySkill(int skillId) {
        int result = skillId / 10000;
        if (skillId / 10000 == 8000) {
            result = skillId / 100;
        }
        return result;
    }

    public static boolean isApplicableSkill(int skil) {
        return ((skil < 80000000 || skil >= 100000000) && (skil % 10000 < 8000 || skil % 10000 > 8006) && !isAngel(skil)) || skil >= 92000000 || (skil >= 80000000 && skil < 80010000); //no additional/decent skills
    }

//    public static boolean isApplicableSkill_(int skil) { //not applicable to saving but is more of temporary
//        for (int i : PlayerStats.pvpSkills) {
//            if (skil == i) {
//                return true;
//            }
//        }
//        return (skil >= 90000000 && skil < 92000000) || (skil % 10000 >= 8000 && skil % 10000 <= 8003) || isAngel(skil);
//    }
    public static boolean isRidingSKill(int skil) {
        return (skil >= 80001000 && skil < 80010000);
    }

    public static boolean isAngel(int skillId) {
        if (MapleJob.isBeginner(skillId / 10000) || skillId / 100000 == 800) {
            switch (skillId % 10000) {
                case 1085: // 大天使 [等級上限：1]\n召喚被大天使祝福封印的大天使。
                case 1087: // 黑天使 [等級上限：1]\n召喚被黑天使祝福封印的大天使。
                case 1090: // 大天使 [等級上限：1]\n召喚被大天使祝福封印的大天使。
                case 1179: // 白色天使 [最高等級： 1]\n召喚出被封印的聖潔天使。
                case 86: // 大天使祝福 [等級上限：1]\n得到大天使的祝福。
                    return true;
            }
        }
        switch (skillId) {
            case 80000052: // 恶魔之息 获得恶魔的力量，攻击力和魔法攻击力增加6，HP、MP增加5%，可以和其他增益叠加。
            case 80000053: // 恶魔召唤 获得恶魔的力量，攻击力和魔法攻击力增加13，HP、MP增加10%，可以和其他增益叠加。
            case 80000054: // 恶魔契约 获得恶魔的力量，攻击力和魔法攻击力增加15，HP、MP增加20%，可以和其他增益叠加。
            case 80000086: // 戰神祝福 [等級上限：1]\n得到戰神的祝福。
            case 80001154: // 白色天使 [最高等級：1]\n召喚被白天使的祝福封印的白天使。
            case 80001262: // 戰神祝福 [等級上限：1]\n召喚戰神
            case 80001518: // 元素瑪瑙 召喚瑪瑙戒指中的#c元素瑪瑙#.
            case 80001519: // 火焰瑪瑙 召喚瑪瑙戒指中的#c火焰瑪瑙#.
            case 80001520: // 閃電瑪瑙 召喚瑪瑙戒指中的#c火焰瑪瑙#.
            case 80001521: // 冰凍瑪瑙 召喚瑪瑙戒指中的#c冰凍瑪瑙#.
            case 80001522: // 大地瑪瑙 召喚瑪瑙戒指中的#c大地瑪瑙#.
            case 80001523: // 黑暗瑪瑙 召喚瑪瑙戒指中的#c黑暗瑪瑙#.
            case 80001524: // 神聖瑪瑙 召喚瑪瑙戒指中的#c神聖瑪瑙#.
            case 80001525: // 火精靈瑪瑙 召喚瑪瑙戒指中的#c火精靈瑪瑙#.
            case 80001526: // 電子瑪瑙 召喚瑪瑙戒指中的#c電子瑪瑙#.
            case 80001527: // 水精靈瑪瑙 召喚瑪瑙戒指中的#c水精靈瑪瑙#.
            case 80001528: // 地精靈瑪瑙 召喚瑪瑙戒指中的#c地精靈瑪瑙#.
            case 80001529: // 惡魔瑪瑙 召喚瑪瑙戒指中的#c惡魔瑪瑙#.
            case 80001530: // 天使瑪瑙 召喚瑪瑙戒指中的#c天使瑪瑙#.
            case 80001715: // 元素瑪瑙
            case 80001716: // 火焰瑪瑙
            case 80001717: // 閃電瑪瑙
            case 80001718: // 冰凍瑪瑙
            case 80001719: // 大地瑪瑙
            case 80001720: // 黑暗瑪瑙
            case 80001721: // 神聖瑪瑙
            case 80001722: // 火精靈瑪瑙
            case 80001723: // 電子精靈瑪瑙
            case 80001724: // 水精靈瑪瑙
            case 80001725: // 地精靈瑪瑙
            case 80001726: // 惡魔瑪瑙
            case 80001727: // 天使瑪瑙
                return true;
        }
        return false;
    }

    public static boolean is紫扇仰波(int id) {
        return id == 42001000 || id > 42001004 && id <= 42001006;
    }

    public static boolean is初心者紫扇仰波(int id) {
        return id == 40021185 || id == 42001006 || id == 80011067;
    }

    public static boolean sub_9F5282(int id) {
        return id == 4221052 || id == 65121052; // 暗影霧殺 || 超級超新星
    }

    public static boolean sub_9F529C(int id) {
        return id == 13121052 // 季風
                || id - 13121052 == 1000000 // 道米尼奧
                || id - 13121052 == 2000000 // 海神降臨
                || id - 13121052 == 66880377 // 崩壞之輪行蹤
                || id - 13121052 == 66880379 // 破滅之輪行蹤
                || (id - 13121052 - 66880379) == 19999852; // 暗影之雨
    }

    public static boolean isKeyDownSkillWithPos(int id) {
        return id == 13111020 || id == 112111016; // 寒冰亂舞 || 旋風飛行
    }

    public static int getHyperAddBullet(int id) {
        if (id == 4121013) { // 四飛閃
            return 4120051; // 四飛閃-攻擊加成
        } else if (id == 5321012) { // 加農砲連擊
            return 5320051; // 加農砲連擊-獎勵攻擊
        }
        return 0;
    }

    public static int getHyperAddAttack(int id) {
        if (id > 12120011) { // 極致熾烈
            if (id > 41121001) { // 神速無雙
                if (id > 61121100) { // 藍焰恐懼
                    if (id > 112101009) { // 電光石火
                        if (id == 112111004) { // 隊伍轟炸
                            return 112120050; // 隊伍轟炸-臨時目標
                        } else if (id > 112119999 && id <= 112120003) { // 朋友發射
                            return 112120053;
                        }
                        return 0;
                    }
                    if (id == 112101009) { // 電光石火
                        return 112120048; // 電光石火-攻擊加成
                    }
                    if (id != 61121201) { // 藍焰恐懼(變身)
                        if (id > 65121006 && (id <= 65121008 || id == 65121101)) { // 三位一體
                            return 65120051; // 三位一體-三重反擊
                        }
                        return 0;
                    }
                } else if (id != 61121100) { // 藍焰恐懼
                    switch (id) {
                        case 41121002: // 一閃
                            return 41120050; // 一閃-次數強化
                        case 41121018: // 瞬殺斬
                        case 41121021: // 瞬殺斬
                            return 41120048; // 瞬殺斬-次數強化
                        case 42121000: // 破邪連擊符
                            return 42120045; // 破邪連擊符-次數強化
                        case 51121007: // 靈魂突擊
                            return 51120051; // 靈魂突擊-獎勵加成
                        case 51121008: // 聖光爆發
                            return 51120048; // 聖光爆發-攻擊加成
                    }
                    return 0;
                }
                return 61120045; // 藍焰恐懼-加碼攻擊
            }
            if (id == 41121001) { // 神速無雙
                return 41120044; // 神速無雙-次數強化
            } else if (id > 21121013) { // 終極之矛
                if (id == 22181002) { // 龍神之怒
                    return 0;//".img/SlideMenu/0/Recommend";
                } else if (id == 25121005) { // 鬼斬
                    return 25120148; // 鬼斬-次數強化
                } else if (id == 31111005) { // 惡魔佈雷斯
                    return 31120044; // 惡魔氣息-攻擊加成
                } else if (id == 31121001) { // 惡魔衝擊
                    return 31120050; // 惡魔衝擊-攻擊加成
                } else if (id == 32111003) {
                    return 0;
                } else if (id == 35121016) { // 巨型火炮：IRON-B
                    return 35120051; // 巨型火炮：IRON-B-追加攻擊
                }
            } else {
                if (id == 21121013) { // 終極之矛
                    // goto LABEL_115;
                }
                if (id == 13121000 + 2) { // 破風之箭
                    return 13120048; // 破風之箭-次數強化
                }
                if (id - (13121000 + 2) == 1000000) { // 四倍緩慢
                    return 14120045; // 五倍緩慢-爆擊率
                }
                if (id - (13121000 + 2) == 1990020 || id - (13121000 + 2) == 1999001) { // 疾風 || 颱風
                    return 15120045; // 疾風-次數強化
                }
                if (id - (13121000 + 2) == 2000000) { // 霹靂
                    return 15120048; // 霹靂-次數強化
                }
                if (id - (13121000 + 2) - 2000000 == 5999002 + 1) { // 終極之矛
                    //LABEL_115:
                    return 21120047; // 終極之矛-加碼攻擊
                } else if (id - (13121000 + 2) - 2000000 - (5999002 + 1) == 1) { // 極冰暴風
                    return 21120049; // 極冰暴風-加碼攻擊
                }
            }
        } else {
            if (id == (12120009 + 2)) { // 極致熾烈
                return 12120046; // 極致熾烈-追加反擊
            }
            if (id <= 5121017) { // 爆烈衝擊波
                if (id >= 5121016) { // 蓄能衝擊波
                    return 5120051; // 蓄能衝擊波-攻擊加成
                }
                if (id <= 3121015) { // 驟雨狂矢
                    switch (id) {
                        case 3121015: // 驟雨狂矢
                            return 3120048; // 驟雨狂矢-攻擊加成
                        case 1120017: // 狂暴攻擊
                        case 1121008: // 狂暴攻擊
                            return 1120051; // 狂暴攻擊-攻擊加成
                        case 1221009: // 騎士衝擊波
                            return 1220048; // 騎士衝擊波-攻擊加成
                        case 1221011: // 鬼神之擊
                            return 1220050; // 鬼神之擊-攻擊加成
                        case 2121003: // 地獄爆發
                            return 2120049; // 地獄爆發-攻擊加成
                        case 2121006: // 梅杜莎之眼
                            return 2120048; // 梅杜莎之眼-次數強化
                        case 2221006: // 閃電連擊
                            return 2220048; // 閃電連擊-攻擊加成
                    }
                    return 0;
                }
                if (id == 3121020) { // 暴風神射
                    return 3120051; // 暴風神射-多重射擊
                }
                if (id == 3221017) { // 光速神弩
                    return 3220048; // 光速神弩-攻擊加成
                }
                if (id == 4221007) { // 瞬步連擊
                    return 4220048; // 瞬步連擊-攻擊加成
                }
                if (id == 4331000) { // 血雨暴風狂斬
                    return 4340045; // 血雨暴風狂斬-攻擊加成
                }
                if (id == 4341009) { // 幻影箭
                    return 4340048; // 幻影箭-攻擊加成
                }
                if (id != 5121007) { // 閃‧連殺
                    return 0;
                }
                return 5120048; // 閃．連殺-攻擊加成
            }
            if (id > 5721064) { // 穿心掌打 
                if (id == (11121101 + 2) || id - (11121101 + 2) == 100) { // 新月分裂 || 太陽穿刺
                    return 11120048; // 分裂與穿刺-次數強化
                } else if (id - (11121101 + 2) == 878923 // 元素火焰
                        || id - (11121101 + 2) == 978925
                        || id - (11121101 + 2) == 988925
                        || id - (11121101 + 2) == 998907) { // 元素火焰 IV
                    return 12120045; // 元素火焰-速發反擊
                }
            } else {
                if (id == 5721064) { // 穿心掌打 
                    return 5720048; // 穿心掌打-次數強化
                }
                if (id == 5121020) { // 閃‧瞬連殺
                    return 5120048; // 閃．連殺-攻擊加成
                }
                if (id - 5121020 == 99996) { // 爆頭射擊
                    return 5220047; // 爆頭射擊-攻擊加成
                } else {
                    if (id - 5121020 == 198991) {
                        //goto LABEL_116;
                    }
                    if (id - 5121020 == 199980) {// 加農砲火箭
                        return 5320048; // 加農砲火箭-攻擊加成
                    }
                    if (id - 5121020 == 199984) { // 雙胞胎猴子
                        //LABEL_116:
                        return 5320043; // 雙胞胎猴子-傷害分裂
                    } else if (id - 5121020 == 600041) { // 龍襲亂舞 
                        return 5720045; // 龍襲亂舞-次數強化
                    }
                }
            }
        }
        return 0;
    }

//    private static List<Integer> SoulSkills;
//    public static List<Integer> getSoulSkills() {
//        if (SoulSkills != null) {
//            return SoulSkills;
//        }
//        SoulSkills = new LinkedList();
//        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
//        for (ItemInformation itInfo : ii.getAllItems()) {
//            if (ItemConstants.類型.靈魂寶珠(itInfo.itemId)) {
//                SoulSkills.add(ii.getSoulSkill(itInfo.itemId));
//            }
//        }
//        return SoulSkills;
//    }
    public static int SkillIncreaseMobCount(int sk) {
        int inc = 0;
        switch (sk) {
            case 112001008://鮮魚龍捲風
            case 112101009://電光石火
            case 112111004://隊伍轟炸
            case 61111100:// 龍劍風
            case 51121008://聖光爆發
            case 42121000://破邪連擊符
            case 41121021://瞬殺斬
            case 41121018://瞬殺斬
            case 41121009://鷹爪閃
            case 36121012://偽裝掃蕩：轟炸
            case 36121011://偽裝掃蕩：砲擊
            case 36121000://疾風劍舞
            case 35121015://巨型火炮：SPLASH-F
            case 33121002://音爆
            case 32121003://颶風
            case 27121303://絕對擊殺
            case 27121202://暗黑烈焰
            case 24121000://連犽突進
            case 24121005://卡牌風暴
            case 15121002://霹靂
            case 13121002://破風之箭
            case 12120011://極致熾烈
            case 11121203://太陽穿刺
            case 11121103://新月分裂
            case 5721007://俠客突襲
            case 5321000://加農砲火箭
            case 5121016://蓄能衝擊波
            case 4341004://短刀護佑
            case 4331000://血雨暴風狂斬
            case 4221007://瞬步連擊
            case 4121017://挑釁契約
            case 3221017://光速神孥
            case 3121015://驟雨狂矢
            case 2221012://冰鋒刃
            case 2221006://閃電連擊
            case 2211007://瞬間移動精通
            case 1211008://雷鳴之劍
            case 1121008://狂暴攻擊
                inc = 2;
                break;
            case 1221004://聖靈之劍
            case 1201012://寒冰之劍
            case 1201011://烈焰之劍
                inc = 3;
                break;
        }
        return inc;
    }
}
