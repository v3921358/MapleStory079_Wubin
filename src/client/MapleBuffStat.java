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

import java.io.Serializable;

public enum MapleBuffStat implements Serializable {

    //DICE = 0x100000, the roll is determined in the long-ass packet itself as buffedvalue
    //ENERGY CHARGE = 0x400000
    //teleport mastery = 0x800000
    //PIRATE'S REVENGE = 0x4000000
    //DASH = 0x8000000 and 0x10000000
    //SPEED INFUSION = 0x40000000
    //MONSTER RIDER = 0x20000000
    //COMBAT ORDERS = 0x1000000
    //物理攻擊力
    WATK(0),
    //物理防禦力
    WDEF(1),
    //魔法攻擊力
    MATK(2),
    //魔法防禦力
    MDEF(3),
    //命中率
    ACC(4),
    //迴避率
    AVOID(5),
    //手技 
    HANDS(6),
    //移動速度 
    SPEED(7),
    //跳躍力
    JUMP(8),
    //魔心防禦 
    MAGIC_GUARD(9),
    //隱藏術  
    DARKSIGHT(10),
    //攻擊加速    
    BOOSTER(11),
    //反射之盾
    POWERGUARD(12),
    //最大HP
    MAXHP(13),
    //最大MP
    MAXMP(14),
    //神聖之光 
    INVINCIBLE(15),
    //無形之箭    
    SOULARROW(16),
    //昏迷
    STUN(17),
    //中毒
    POISON(18),
    //封印
    SEAL(19),
    //黑暗
    DARKNESS(20),
    //鬥氣集中
    COMBO(21),
    //召喚獸
    SUMMON(21), //hack buffstat for summons ^.- (does/should not increase damage... hopefully <3)
    //屬性攻擊
    WK_CHARGE(22),
    //龍之力量 ? 需要測試
    DRAGONBLOOD(23),
    //神聖祈禱
    HOLY_SYMBOL(24),
    //幸運術
    MESOUP(25),
    //影分身
    SHADOWPARTNER(26),
    //勇者掠奪術
    PICKPOCKET(27),
    //替身術
    PUPPET(28), // HACK - shares buffmask with pickpocket - odin special ^.-
    //楓幣護盾
    MESOGUARD(29),
    //虛弱 
    WEAKEN(30),
    //詛咒
    CURSE(31),
    //緩慢 
    SLOW(32),
    //變身  
    MORPH(33),
    //恢復
    RECOVERY(34),
    //空氣齡
    HP_LOSS_GUARD(34),
    //楓葉祝福  
    MAPLE_WARRIOR(35),
    //格擋(穩如泰山)   
    STANCE(36),
    //銳利之眼  
    SHARP_EYES(37),
    //魔法反擊
    MANA_REFLECTION(38),
    //誘惑  
    DRAGON_ROAR(39),
    //暗器傷人
    SPIRIT_CLAW(40),
    //魔力無限
    INFINITY(41),
    //進階祝福    
    HOLY_SHIELD(42),
    //敏捷提升    
    HAMSTRING(43),
    //命中率增加
    BLIND(44),
    //集中精力
    CONCENTRATE(45),
    //不死化
    ZOMBIFY(46),
    //英雄的回響  
    ECHO_OF_HERO(47),//0x8000
    UNKNOWN3(48),//0x10000
    MESO_RATE(48),//0x10000
    GHOST_MORPH(49),//0x20000
    ARIANT_COSS_IMU(50), // 0x40000
    DROP_RATE(52),
    //MESO_RATE(53),
    EXPRATE(54),
    ACASH_RATE(55),
    GM_HIDE(56),
    UNKNOWN7(57),
    ILLUSION(58),
    //地火天爆
    BERSERK_FURY(57),//59
    //金剛不壞
    DIVINE_BODY(60),//60
    SPARK(59),
    ARIANT_COSS_IMU2(62), // no idea, seems the same
    //終極攻擊
    FINALATTACK(61),
    //自然力重置     
    ELEMENT_RESET(63),
    //(CMS_风影漫步)
    WIND_WALK(64),
    // 矛之鬥氣
    ARAN_COMBO(66),//68
    // 吸血術
    COMBO_DRAIN(67),//69
    // 宙斯之盾
    COMBO_BARRIER(68),//70
    // 強化連擊
    BODY_PRESSURE(69),//71
    // 精準擊退
    SMART_KNOCKBACK(70),//72
    SOUL_STONE(73), //same as pyramid_pq
    ENERGY_CHARGE(77),//75
    DASH_SPEED(78),
    DASH_JUMP(79),
    MONSTER_RIDING(80),
    SPEED_INFUSION(81),
    HOMING_BEACON(82),
    SOARING(82),
    FREEZE(83),
    LIGHTNING_CHARGE(84),
    MIRROR_IMAGE(85),
    OWL_SPIRIT(86), //POST BB
    召唤玩家1(77),
    召唤玩家2(78),
    召唤玩家3(79),
    召唤玩家4(80),
    召唤玩家5(81),
    召唤玩家6(82),
    召唤玩家7(83),
    召唤玩家8(84), //DUMMY_STAT0     (0x8000000L, true), //appears on login
    //DUMMY_STAT1     (0x10000000L, true),
    //DUMMY_STAT2     (0x20000000L, true),
    //DUMMY_STAT3     (0x40000000L, true),
    //DUMMY_STAT4     (0x80000000L, true),
    //db stuff
    /*FINAL_CUT(88),
     THORNS(89),
     ENHANCED_MAXHP(93),
     ENHANCED_MAXMP(94),
     ENHANCED_WATK(95),
     ENHANCED_WDEF(96),
     ENHANCED_MDEF(97),
     PERFECT_ARMOR(98),
     SATELLITESAFE_PROC(99),
     SATELLITESAFE_ABSORB(100),
     CRITICAL_RATE_BUFF(102),
     MP_BUFF(103),
     DAMAGE_TAKEN_BUFF(104),
     DODGE_CHANGE_BUFF(105),
     CONVERSION(106),
     REAPER(107),
     MECH_CHANGE(109), //determined in packet by [skillLevel or something] [skillid] 1E E0 58 52???
     DARK_AURA(111),
     BLUE_AURA(112),
     YELLOW_AURA(113),*/;

    private static final long serialVersionUID = 0L;
    private final int buffstat;
    private final int first;
    private final long oldvalue;

    private MapleBuffStat(int buffstat) {
        this.buffstat = 1 << (buffstat % 32);
        this.first = 3 - (int) Math.floor(buffstat / 32);
        this.oldvalue = new Long(this.buffstat) << (32 * (first % 2 + 1));
    }

    private MapleBuffStat(int buffstat, boolean stacked) {
        this.buffstat = 1 << ((buffstat % 32));
        this.first = (int) Math.floor(buffstat / 32);
        this.oldvalue = new Long(this.buffstat) << (32 * (first % 2 + 1));
    }

    public final long getOldValue() {
        return this.oldvalue;
    }

    public final int getPosition() {
        return first;
    }
    
    public final int getPosition(boolean fromZero) {
        if (!fromZero) {
            return this.first;
        }
        switch (this.first) {
            case 4:
                return 0;
            case 3:
                return 1;
            case 2:
                return 2;
            case 1:
                return 3;
        }
        return 0;
    }

    public final int getValue() {
        return buffstat;
    }
}
