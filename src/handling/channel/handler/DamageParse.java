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
package handling.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import client.ISkill;
import constants.GameConstants;
import client.inventory.IItem;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleJob;
import client.inventory.MapleInventoryType;
import client.PlayerStats;
import client.SkillFactory;
import client.anticheat.CheatTracker;
import client.anticheat.CheatingOffense;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ServerConfig;
import constants.SkillType;
import constants.SkillType.聖魂劍士2;
import constants.SkillType.閃雷悍將3;
import handling.world.World;
import java.util.Map;
import server.MapleStatEffect;
import server.Randomizer;
import server.life.Element;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MaplePvp;
import tools.MaplePacketCreator;
import tools.AttackPair;
import tools.FileoutputUtil;
import tools.Pair;
import tools.data.LittleEndianAccessor;

public class DamageParse {

    public static void applyAttack(final AttackInfo attack, final ISkill theSkill, final MapleCharacter player, int attackCount, final double maxDamagePerMonster, final MapleStatEffect effect, final AttackType attack_type) {
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        if (attack.real) {
            player.getCheatTracker().checkAttack(attack.skill, attack.lastAttackTickCount);
        }

        if (attack.skill != 0) {
            if (effect == null) {
                player.getClient().sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            /* 武陵道場技能 */
            if (GameConstants.isMulungSkill(attack.skill)) {
                if (player.getmulungEnergy() < 10000) {
                    return;
                }
                if (player.getMapId() / 10000 == 92502) {
                    player.mulungEnergyModify(false);
                } else {
                    return;
                }
            }
            /* 金字塔技能 */
            if (GameConstants.isPyramidSkill(attack.skill)) {
                if (player.getMapId() / 1000000 != 926
                        || player.getPyramidSubway() == null || !player.getPyramidSubway().onSkillUse(player)) {
                    //AutobanManager.getInstance().autoban(player.getClient(), "Using Pyramid skill outside of pyramid maps.");
                    return;
                }
            }
            if (GameConstants.isAran(player.getJob())) {
                int reduce = player.Aran_ReduceCombo(attack.skill);
                if (reduce > 0) {
                    player.setCombo(player.getCombo() - reduce);
                }
            }
            int last = attackCount;
            boolean mirror_fix = false;
            if (player.getJob() >= 411 && player.getJob() <= 412) {
                mirror_fix = true;
            }

            if (player.getJob() >= 1400 && player.getJob() <= 1412) {
                mirror_fix = true;
            }
            if (attack.skill == 聖魂劍士2.靈魂之刃) {
                last = 2;
            }
            
            if (attack.skill == 閃雷悍將3.鯨噬) {
                last = 3;
            }
            if (mirror_fix) {
                last *= 2;
            }
            if (attack.hits > last) {
                if (player.hasGmLevel(1)) {
                    player.dropMessage("攻擊次數異常攻擊次數 " + attack.hits + " 服務端判斷正常攻擊次數 " + last + " 技能ID " + attack.skill);
                } else {
                    //player.ban(player.getName() + "技能攻擊次數異常", true, true, false);
                    //player.getClient().getSession().close();
                    String reason = "使用違法程式練功";
                    //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為" + reason + "而被管理員永久停權。"));
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") 攻擊次數異常已自動封鎖。 玩家攻擊次數 " + attack.hits + " 服務端判斷正常攻擊次數 " + last + " 技能ID " + attack.skill));
                    FileoutputUtil.logToFile("logs/Hack/Ban/技能攻擊次數.txt", "\r\n" + FileoutputUtil.NowTime() + "玩家: " + player.getName() + "(" + player.getLevel() + ") 地圖: " + player.getMapId() + " 技能代碼: " + attack.skill + " 技能等級: " + player.getSkillLevel(attack.skill) + " 攻擊次數 : " + attack.hits + " 正常攻擊次數 :" + last);
                    //return;
                }
            }

            /* 確認是否超過打怪數量*/
            int CheckCount = effect.getMobCount();
            if (attack.targets > CheckCount) {
                if (player.hasGmLevel(1)) {
                    player.dropMessage("打怪數量異常,技能代碼: " + attack.skill + " 封包怪物量 : " + attack.targets + " 服務端怪物量 :" + CheckCount);
                } else {
                    FileoutputUtil.logToFile("logs/Hack/Ban/打怪數量異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家: " + player.getName() + "(" + player.getLevel() + ") 地圖: " + player.getMapId() + "技能代碼: " + attack.skill + " 技能等級: " + player.getSkillLevel(effect.getSourceId()) + " 封包怪物量 : " + attack.targets + " 服務端怪物量 :" + CheckCount);
                    //player.ban(player.getName() + "打怪數量異常", true, true, false);
                    //player.getClient().getSession().close();
                    String reason = "使用違法程式練功";
                    //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為" + reason + "而被管理員永久停權。"));
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") " + "攻擊怪物數量異常。 " + "封包怪物量 " + attack.targets + " 服務端怪物量 " + CheckCount + " 技能ID " + attack.skill));
                    //return;
                }
            }

        }

        if (attack.hits > 0 && attack.targets > 0) {
            // Don't ever do this. it's too expensive.
            if (!player.getStat().checkEquipDurabilitys(player, -1)) { //i guess this is how it works ?
                player.dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
                return;
            } //lol
        }

        int totDamage = 0;
        final MapleMap map = player.getMap();

        if (attack.skill == SkillType.神偷.楓幣炸彈) { // meso explosion
            for (AttackPair oned : attack.allDamage) {
                if (oned.attack != null) {
                    continue;
                }
                final MapleMapObject mapobject = map.getMapObject(oned.objectid, MapleMapObjectType.ITEM);

                if (mapobject != null) {
                    final MapleMapItem mapitem = (MapleMapItem) mapobject;
                    mapitem.getLock().lock();
                    try {
                        if (mapitem.getMeso() > 0) {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            map.removeMapObject(mapitem);
                            map.broadcastMessage(MaplePacketCreator.explodeDrop(mapitem.getObjectId()));
                            mapitem.setPickedUp(true);
                        } else {
                            player.getCheatTracker().registerOffense(CheatingOffense.ETC_EXPLOSION);
                            return;
                        }
                    } finally {
                        mapitem.getLock().unlock();
                    }
                } else {
                    player.getCheatTracker().registerOffense(CheatingOffense.EXPLODING_NONEXISTANT);
                    return; // etc explosion, exploding nonexistant things, etc.
                }
            }
        }
        int fixeddmg, totDamageToOneMonster = 0;
        long hpMob = 0;
        final PlayerStats stats = player.getStat();
        int CriticalDamage = stats.passive_sharpeye_percent();
        byte ShdowPartnerAttackPercentage = 0;

        if (attack_type == AttackType.RANGED_WITH_SHADOWPARTNER || attack_type == AttackType.NON_RANGED_WITH_MIRROR) {
            final MapleStatEffect shadowPartnerEffect;
            if (attack_type == AttackType.NON_RANGED_WITH_MIRROR) {
                shadowPartnerEffect = player.getStatForBuff(MapleBuffStat.MIRROR_IMAGE);
            } else {
                shadowPartnerEffect = player.getStatForBuff(MapleBuffStat.SHADOWPARTNER);
            }

            if (shadowPartnerEffect != null) {
                if (attack.skill != 0 && attack_type != AttackType.NON_RANGED_WITH_MIRROR) {
                    ShdowPartnerAttackPercentage = (byte) shadowPartnerEffect.getY();
                } else {
                    ShdowPartnerAttackPercentage = (byte) shadowPartnerEffect.getX();
                }
            }
            attackCount /= 2; // hack xD
        }

        byte overallAttackCount; // Tracking of Shadow Partner additional damage.
        double maxDamagePerHit = 0;
        MapleMonster monster;
        MapleMonsterStats monsterstats;
        boolean Tempest;

        for (final AttackPair oned : attack.allDamage) {
            monster = map.getMonsterByOid(oned.objectid);

            if (monster != null) {
                totDamageToOneMonster = 0;
                hpMob = monster.getHp();
                monsterstats = monster.getStats();
                fixeddmg = monsterstats.getFixedDamage();
                Tempest = monster.getStatusSourceID(MonsterStatus.FREEZE) == 21120006;
                maxDamagePerHit = calculateMaxWeaponDamagePerHit(player, monster, attack, theSkill, effect, maxDamagePerMonster, CriticalDamage);
                overallAttackCount = 0;
                Integer eachd;
                for (Pair<Integer, Boolean> eachde : oned.attack) {
                    eachd = eachde.left;
                    overallAttackCount++;
                    /* 確認是否超過預計傷害*/
                    if (!GameConstants.isElseSkill(attack.skill)) {
                        if (GameConstants.Novice_Skill(attack.skill)) {//新手技能
                            if (eachd > 40) {
                                boolean apple = false;
                                if (player.getBuffSource(MapleBuffStat.WATK) == 2022179 || player.getBuffSource(MapleBuffStat.MATK) == 2022179 || player.getBuffSource(MapleBuffStat.WDEF) == 2022179) {
                                    apple = true;
                                }
                                FileoutputUtil.logToFile("logs/Hack/Ban/傷害異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家<" + player.getLevel() + ">: " + player.getName() + " 怪物 " + monster.getId() + " 地圖: " + player.getMapId() + " 技能代碼: " + attack.skill + " 最高傷害: 40 本次傷害 :" + eachd + " 預計傷害: " + (int) maxDamagePerHit + "是否為BOSS: " + monster.getStats().isBoss() + " 紫色蘋果: " + apple);
                                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為傷害異常而被管理員永久停權。"));
                                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") " + "傷害異常。 " + "最高傷害 40 本次傷害 " + eachd + " 技能ID " + attack.skill));
                                //player.ban(player.getName() + "傷害異常", true, true, false);
                                //player.getClient().getSession().close();
                                //return;
                            }
                        }
                        boolean ban = false;
                        int atk = 20000000;

                        if (!GameConstants.isAran(player.getJob())) {
                            if (player.getLevel() < 10) {
                                atk = 250;
                            } else if (player.getLevel() <= 20) {
                                atk = 1000;
                            } else if (player.getLevel() <= 30) {
                                atk = 2500;
                            } else if (player.getLevel() <= 60) {
                                atk = 8000;
                            }
                            if (attack.skill == 1001004 || attack.skill == 11001002 || attack.skill == 5111002 || attack.skill == 15101005) {
                                atk *= 2;
                            }

                            // maxDamagePerHit 利用 defined 變數控制是否超過19萬攻擊
                            if (eachd >= atk && eachd > Math.ceil(maxDamagePerHit * 1.2)) {
                                ban = true;
                            }
                            if (eachd == monster.getMobMaxHp()) {
                                ban = false;
                            }
                            if (player.hasGmLevel(1)) {
                                ban = false;
                            }
                            if (ban) {
                                boolean apple = false;
                                if (player.getBuffSource(MapleBuffStat.WATK) == 2022179 || player.getBuffSource(MapleBuffStat.MATK) == 2022179 || player.getBuffSource(MapleBuffStat.WDEF) == 2022179) {
                                    apple = true;
                                }
                                FileoutputUtil.logToFile("logs/Hack/Ban/傷害異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家<" + player.getLevel() + ">: " + player.getName() + " 怪物 " + monster.getId() + " 地圖: " + player.getMapId() + " 技能代碼: " + attack.skill + " 最高傷害: " + atk + " 本次傷害 :" + eachd + " 預計傷害: " + (int) maxDamagePerHit + "是否為BOSS: " + monster.getStats().isBoss() + " 紫色蘋果: " + apple);
                                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為傷害異常而被管理員永久停權。"));
                                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") " + "傷害異常。 " + "最高傷害 " + atk + " 本次傷害 " + eachd + " 技能ID " + attack.skill));
                                //player.ban(player.getName() + "傷害異常", true, true, false);
                                //player.getClient().getSession().close();
                                //return;
                            }
                        }
                        atk = GameConstants.getMaxDamage(player.getLevel(), player.getJob(), attack.skill);
                        if (GameConstants.isAran(player.getJob())) {
                            if (player.getLevel() > 10) {
                                if (eachd > atk && eachd > maxDamagePerHit * 2) {
                                    FileoutputUtil.logToFile("logs/Hack/傷害異常_狂狼.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家<" + player.getLevel() + ">: " + player.getName() + " 技能代碼: " + attack.skill + " 怪物: " + monster.getId() + " 最高傷害: " + atk + " 本次傷害 :" + eachd + " 預計傷害: " + (int) maxDamagePerHit + "是否為BOSS: " + monster.getStats().isBoss());
                                }
                            }
                            if (player.getLevel() <= 20) {
                                atk = 1000;
                                if (eachd >= atk && eachd > maxDamagePerHit) {
                                    boolean apple = false;
                                    if (player.getBuffSource(MapleBuffStat.WATK) == 2022179 || player.getBuffSource(MapleBuffStat.MATK) == 2022179 || player.getBuffSource(MapleBuffStat.WDEF) == 2022179) {
                                        apple = true;
                                    }
                                    ban = true;
                                    FileoutputUtil.logToFile("logs/Hack/Ban/傷害異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家<" + player.getLevel() + ">: " + player.getName() + " 怪物 " + monster.getId() + " 地圖: " + player.getMapId() + " 技能代碼: " + attack.skill + " 最高傷害: " + atk + " 本次傷害 :" + eachd + " 預計傷害: " + (int) maxDamagePerHit + "是否為BOSS: " + monster.getStats().isBoss() + " 紫色蘋果: " + apple);
                                    //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為傷害異常而被管理員永久停權。"));
                                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") " + "傷害異常。 " + "最高傷害 " + atk + " 本次傷害 " + eachd + " 技能ID " + attack.skill));
                                    //player.ban(player.getName() + "傷害異常", true, true, false);
                                    //player.getClient().getSession().close();
                                    //return;
                                }
                            }

                        } else if (eachd > atk && eachd > maxDamagePerHit * 2) {
                            FileoutputUtil.logToFile("logs/Hack/傷害異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家<" + player.getLevel() + ">: " + player.getName() + " 技能代碼: " + attack.skill + " 怪物: " + monster.getId() + " 最高傷害: " + atk + " 本次傷害 :" + eachd + " 預計傷害: " + (int) maxDamagePerHit + "是否為BOSS: " + monster.getStats().isBoss());
                        }
                    }
                    if (overallAttackCount - 1 == attackCount) {
                        double min = maxDamagePerHit;
                        double shadow = (ShdowPartnerAttackPercentage == 0D ? 1D : ShdowPartnerAttackPercentage);
                        if (ShdowPartnerAttackPercentage != 0) {
                            min = maxDamagePerHit / 100.0D;
                        }
                        double dam = (monsterstats.isBoss() ? stats.bossdam_r : stats.dam_r);
                        double last = min * (shadow * dam / 100.0D);
                        maxDamagePerHit = last;
                    }
                    /*if (fixeddmg != -1) {
                        if (monsterstats.getOnlyNoramlAttack()) {
                            eachd = attack.skill != 0 ? 0 : fixeddmg;
                        } else {
                            eachd = fixeddmg;
                        }
                    } else if (monsterstats.getOnlyNoramlAttack()) {
                        eachd = attack.skill != 0 ? 0 : Math.min(eachd, (int) maxDamagePerHit);  // Convert to server calculated damage
                    } else if (!player.isGM()) {
                        if (Tempest) { // Monster buffed with Tempest
                            if (eachd > monster.getMobMaxHp()) {
                                eachd = (int) Math.min(monster.getMobMaxHp(), Integer.MAX_VALUE);
                                player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE);
                            }
                        } else if (!monster.isBuffed(MonsterStatus.DAMAGE_IMMUNITY) && !monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY) && !monster.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT)) {
                            if (eachd > maxDamagePerHit) {
                                player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE, new StringBuilder().append("[傷害: ").append(eachd).append(", 預期: ").append(maxDamagePerHit).append(", 怪物: ").append(monster.getId()).append("] [職業: ").append(player.getJob()).append(", 等級: ").append(player.getLevel()).append(", 使用的技能: ").append(attack.skill).append("]").toString());
                                if (eachd > maxDamagePerHit * 2) {
                                    if (eachd > maxDamagePerHit * 2.0D && maxDamagePerHit != 1) {
                                        if (ServerConfig.LOG_DAMAGE) {
                                            FileoutputUtil.logToFile("Logs/hack/傷害計算/傷害計算修正_" + monster.getId() + "_" + attack.skill + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家: " + player.getName() + "(" + player.getLevel() + ") 職業: " + player.getJob() + " 怪物:" + monster.getId() + " 封包傷害 :" + eachd + " 預計傷害 :" + (int) maxDamagePerHit + " 是否為BOSS: " + monster.getStats().isBoss(), false, false);
                                        }
                                        player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_2, new StringBuilder().append("[傷害: ").append(eachd).append(", 預計傷害: ").append((int) maxDamagePerHit).append(", 怪物: ").append(monster.getId()).append("] [職業: ").append(player.getJob()).append(", 等級: ").append(player.getLevel()).append(", 技能: ").append(attack.skill).append("]").toString());
                                    }
                                    eachd = (int) (maxDamagePerHit * 2); // Convert to server calculated damage
                                    player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_2, new StringBuilder().append("[傷害: ").append(eachd).append(", 預期: ").append(maxDamagePerHit).append(", 怪物: ").append(monster.getId()).append("] [職業: ").append(player.getJob()).append(", 等級: ").append(player.getLevel()).append(", 使用的技能: ").append(attack.skill).append("]").toString());
                                    if (eachd >= 10000) {
                                        player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_2, new StringBuilder().append("[傷害: ").append(eachd).append(", 預期: ").append(maxDamagePerHit).append(", 怪物: ").append(monster.getId()).append("] [職業: ").append(player.getJob()).append(", 等級: ").append(player.getLevel()).append(", 使用的技能: ").append(attack.skill).append("]").toString());
                                    }
                                }
                            }
                        } else if (eachd > maxDamagePerHit) {
                            eachd = (int) (maxDamagePerHit);
                            if (eachd > maxDamagePerHit * 2.0D && maxDamagePerHit != 1) {
                                if (ServerConfig.LOG_DAMAGE) {
                                    FileoutputUtil.logToFile("Logs/hack/傷害計算/傷害計算修正_" + monster.getId() + "_" + attack.skill + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家: " + player.getName() + "(" + player.getLevel() + ") 職業: " + player.getJob() + " 怪物:" + monster.getId() + " 封包傷害 :" + eachd + " 預計傷害 :" + (int) maxDamagePerHit + " 是否為BOSS: " + monster.getStats().isBoss());
                                }
                                player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_2, new StringBuilder().append("[傷害: ").append(eachd).append(", 預計傷害: ").append((int) maxDamagePerHit).append(", 怪物: ").append(monster.getId()).append("] [職業: ").append(player.getJob()).append(", 等級: ").append(player.getLevel()).append(", 技能: ").append(attack.skill).append("]").toString());
                            }
                        }
                    }*/
                    totDamageToOneMonster += eachd;
                    //force the miss even if they dont miss. popular wz edit
                    if (monster.getId() == 9300021 && player.getPyramidSubway() != null) { //miss
                        player.getPyramidSubway().onMiss(player);
                    }
                }
                totDamage += totDamageToOneMonster;
                player.checkMonsterAggro(monster);
                double range = player.getPosition().distanceSq(monster.getPosition());
                double SkillRange = GameConstants.getAttackRange(player, effect, attack);
                if (player.getDebugMessage() && range > SkillRange) {
                    player.dropMessage("技能[" + attack.skill + "] 預計範圍: " + (int) SkillRange + " 實際範圍: " + (int) range + "");
                }
                if (range > SkillRange && !player.inBossMap()) { // 815^2 <-- the most ranged attack in the game is Flame Wheel at 815 range
                    player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, "攻擊範圍異常,技能:" + attack.skill + "(" + SkillFactory.getName(attack.skill) + ")　怪物:" + monster.getId() + " 正常範圍:" + (int) SkillRange + " 計算範圍:" + (int) range); // , Double.toString(Math.sqrt(distance))
                    if (range > SkillRange * 2) {
                        player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER_BAN, "超大攻擊範圍,技能:" + attack.skill + "(" + SkillFactory.getName(attack.skill) + ")　怪物:" + monster.getId() + " 正常範圍:" + (int) SkillRange + " 計算範圍:" + (int) range); // , Double.toString(Math.sqrt(distance))
                    }
                    return;
                }
                // pickpocket
                if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null) {
                    switch (attack.skill) {
                        case 0:
                        case 4001334:
                        case 4201005:
                        case 4211002:
                        case 4211004:
                        case 4221003:
                        case 4221007:
                            handlePickPocket(player, monster, oned);
                            break;
                    }
                }
                final MapleStatEffect ds = player.getStatForBuff(MapleBuffStat.DARKSIGHT);
                if (ds != null) {
                    if (ds.getSourceId() != 9001004) {
                        if (ds.getSourceId() != 4330001 || !ds.makeChanceResult()) {
                            player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                        }
                    }
                }
                final MapleStatEffect wd = player.getStatForBuff(MapleBuffStat.WIND_WALK);
                if (wd != null) {
                    if (player.getJob() >= 1300 && player.getJob() <= 1312) {
                        player.cancelEffectFromBuffStat(MapleBuffStat.WIND_WALK);
                    }
                }

                if (totDamageToOneMonster > 0) {
                    if (attack.skill != 1221011) {
                        monster.damage(player, totDamageToOneMonster, true, attack.skill);
                    } else {
                        monster.damage(player, (monster.getStats().isBoss() ? 500000 : (monster.getHp() - 1)), true, attack.skill);
                    }
                    if (monster.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT)) { //test
                        player.addHP(-(7000 + Randomizer.nextInt(8000))); //this is what it seems to be?
                    }
                    if (stats.hpRecoverProp > 0) {
                        if (Randomizer.nextInt(100) <= stats.hpRecoverProp) {//i think its out of 100, anyway
                            player.healHP(stats.hpRecover);
                        }
                    }
                    if (stats.mpRecoverProp > 0) {
                        if (Randomizer.nextInt(100) <= stats.mpRecoverProp) {//i think its out of 100, anyway
                            player.healMP(stats.mpRecover);
                        }
                    }
                    if (player.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
                        stats.setHp((stats.getHp() + ((int) Math.min(monster.getMobMaxHp(), Math.min(((int) ((double) totDamage * (double) player.getStatForBuff(MapleBuffStat.COMBO_DRAIN).getX() / 100.0)), stats.getMaxHp() / 2)))), true);
                    }

                    int[] skillsl = {SkillType.夜使者.飛毒殺, SkillType.暗影神偷.飛毒殺, SkillType.暗夜行者3.飛毒殺};
                    for (int i : skillsl) {
                        final ISkill skill = SkillFactory.getSkill(i);
                        if (player.getSkillLevel(skill) > 0) {
                            final MapleStatEffect venomEffect = skill.getEffect(player.getSkillLevel(skill));
                            if (venomEffect.makeChanceResult()) {
                                monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.POISON, 1, i, null, false), true, venomEffect.getDuration(), monster.getStats().isBoss(), venomEffect);
                            }
                            break;
                        }
                    }
                    // effects
                    switch (attack.skill) {

                        case SkillType.刺客.吸血術: //drain
                        case SkillType.閃雷悍將3.損人利己:
                        case SkillType.格鬥家.損人利己: { // Energy Drain
                            int getHP = ((int) Math.min(monster.getMobMaxHp(), Math.min(((int) ((double) totDamage * (double) theSkill.getEffect(player.getSkillLevel(theSkill)).getX() / 100.0)), stats.getMaxHp() / 2)));
                            stats.setHp(stats.getHp() + getHP, true);
                            break;
                        }
                        case SkillType.神槍手.指定攻擊:
                        case SkillType.槍神.精準砲擊: {//homing
                            player.setLinkMid(monster.getObjectId());
                            break;
                        }
                        case SkillType.龍騎士.龍之獻祭: { // Sacrifice
                            final int remainingHP = stats.getHp() - totDamage * (effect != null ? effect.getX() : 0) / 100;
                            stats.setHp(remainingHP > 1 ? remainingHP : 1);
                            break;
                        }

                        case SkillType.盜賊.雙飛斬:
                        case SkillType.盜賊.詛咒術:
                        case SkillType.盜賊.劈空斬:
                        case SkillType.暗殺者.風魔手裏劍:
                        case SkillType.夜使者.三飛閃:
                        case SkillType.俠盜.迴旋斬:
                        case SkillType.神偷.落葉斬:
                        case SkillType.神偷.分身術:
                        case SkillType.暗影神偷.瞬步連擊:
                        case SkillType.暗影神偷.致命暗殺:
                        case SkillType.暗夜行者1.詛咒術:
                        case SkillType.暗夜行者1.雙飛斬:
                        case SkillType.暗夜行者3.風魔手裏劍:
                        case SkillType.暗夜行者3.三飛閃: {
                            if (player.hasBuffedValue(MapleBuffStat.WK_CHARGE) && !monster.getStats().isBoss()) {
                                MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.WK_CHARGE);
                                if (eff != null) {
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.SPEED, eff.getX(), eff.getSourceId(), null, false), false, eff.getY() * 1000, monster.getStats().isBoss(), eff);
                                }
                            }
                            if (player.hasBuffedValue(MapleBuffStat.BODY_PRESSURE) && !monster.getStats().isBoss()) {
                                MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.BODY_PRESSURE);
                                if ((eff != null) && (eff.makeChanceResult()) && (!monster.isBuffed(MonsterStatus.NEUTRALISE))) {
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.NEUTRALISE, 1, eff.getSourceId(), null, false), false, eff.getX() * 1000, monster.getStats().isBoss(), eff);
                                }
                            }
                            int[] skills = {SkillType.夜使者.飛毒殺, SkillType.暗影神偷.飛毒殺, SkillType.暗夜行者3.飛毒殺};
                            for (int i : skills) {
                                final ISkill skill = SkillFactory.getSkill(i);
                                if (player.getSkillLevel(skill) > 0) {
                                    final MapleStatEffect venomEffect = skill.getEffect(player.getSkillLevel(skill));
                                    if (venomEffect.makeChanceResult()) {
                                        monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.POISON, 1, i, null, false), true, venomEffect.getDuration(), monster.getStats().isBoss(), venomEffect);
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case SkillType.俠盜.妙手術:  //steal
                            monster.handleSteal(player);
                            break;
                        //case 21101003: // body pressure
                        case SkillType.狂狼勇士1.雙重攻擊: // Double attack
                        case SkillType.狂狼勇士2.三重攻擊: // Triple Attack
                        case SkillType.狂狼勇士2.突刺之矛: // Pole Arm Push
                        case SkillType.狂狼勇士2.猛擲之矛: // Pole Arm Smash
                        case SkillType.狂狼勇士3.伺機攻擊: // Full Swing
                        case SkillType.狂狼勇士3.挑怪: // Pole Arm Toss
                        case SkillType.狂狼勇士3.狼魂衝擊: // Fenrir Phantom
                        case SkillType.狂狼勇士3.旋風斬: // Whirlwind
                        case SkillType.狂狼勇士3.雙重攻擊: // (hidden) Full Swing - Double Attack
                        case SkillType.狂狼勇士3.三重攻擊: // (hidden) Full Swing - Triple Attack
                        case SkillType.狂狼勇士4.終極攻擊: // Overswing
                        case SkillType.狂狼勇士4.終極之矛: // Pole Arm finale
                        case SkillType.狂狼勇士4.極冰暴風: // Tempest
                        case SkillType.狂狼勇士4.雙重攻擊: // (hidden) Overswing - Double Attack
                        case SkillType.狂狼勇士4.三重攻擊:
                            if ((player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) && (!monster.getStats().isBoss())) {
                                MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.WK_CHARGE);
                                if (eff != null) {
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.SPEED, eff.getX(), eff.getSourceId(), null, false), false, eff.getY() * 1000, monster.getStats().isBoss(), eff);
                                }
                            }
                            if ((player.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null) && (!monster.getStats().isBoss())) {
                                MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.BODY_PRESSURE);

                                if ((eff != null) && (eff.makeChanceResult()) && (!monster.isBuffed(MonsterStatus.NEUTRALISE))) {
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.NEUTRALISE, 1, eff.getSourceId(), null, false), false, eff.getX() * 1000, true, eff);
                                }
                            }
                            break;
                    }
                    if (totDamageToOneMonster > 0) {
                        IItem weapon_ = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                        if (weapon_ != null) {
                            MonsterStatus stat = GameConstants.getStatFromWeapon(weapon_.getItemId());
                            if ((stat != null) && (Randomizer.nextInt(100) < GameConstants.getStatChance())) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(stat, Integer.valueOf(GameConstants.getXForStat(stat)), GameConstants.getSkillForStat(stat), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, 10000L, monster.getStats().isBoss(), null);
                            }
                        }
                        if (player.hasBuffedValue(MapleBuffStat.BLIND)) {
                            MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.BLIND);

                            if ((eff != null) && (eff.makeChanceResult())) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.ACC, Integer.valueOf(eff.getX()), eff.getSourceId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 1000, monster.getStats().isBoss(), eff);
                            }
                        }

                        if (player.hasBuffedValue(MapleBuffStat.HAMSTRING)) {
                            MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.HAMSTRING);

                            if ((eff != null) && (eff.makeChanceResult())) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.SPEED, Integer.valueOf(eff.getX()), 3121007, null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 1000, monster.getStats().isBoss(), eff);
                            }
                        }

                        if ((player.getJob() == 121) || (player.getJob() == 122)) {
                            ISkill skill = SkillFactory.getSkill(1211006);
                            if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, skill)) {
                                MapleStatEffect eff = skill.getEffect(player.getSkillLevel(skill));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.FREEZE, Integer.valueOf(1), skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 2000, monster.getStats().isBoss(), eff);
                            }
                            skill = SkillFactory.getSkill(1211005);
                            if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, skill)) {
                                MapleStatEffect eff = skill.getEffect(player.getSkillLevel(skill));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.FREEZE, Integer.valueOf(1), skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 2000, monster.getStats().isBoss(), eff);
                            }

                        }
                    }
                    if (effect != null && (effect.getMonsterStati().size() > 0) && effect.makeChanceResult()) {
                        for (Map.Entry z : effect.getMonsterStati().entrySet()) {
                            monster.applyStatus(player, new MonsterStatusEffect((MonsterStatus) z.getKey(), (Integer) z.getValue(), theSkill.getId(), null, false), effect.isPoison(), effect.getDuration(), monster.getStats().isBoss(), effect);
                        }
                    }
                }
            }
        }

        if (effect != null && attack.skill != 0 && (attack.targets > 0 || (attack.skill != 4331003 && attack.skill != 4341002)) && attack.skill != 21101003 && attack.skill != 5110001 && attack.skill != 15100004 && attack.skill != 11101002 && attack.skill != 13101002 && attack.skill != 14111006) {
            effect.applyTo(player, attack.position);
        }
        if (ServerConfig.isPvPChannel(player.getClient().getChannel())) {
            if (player.getMapId() == 100000000) {
                MaplePvp.doPvP(player, map, attack);
            }
        }

        if (attack.skill == 14111006) {
            effect.applyTo(player, attack.positionxy);
        }
        if (totDamage > 1) {
            final CheatTracker tracker = player.getCheatTracker();

            tracker.setAttacksWithoutHit(true);
            if (tracker.getAttacksWithoutHit() > 50) {
                tracker.registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, Integer.toString(tracker.getAttacksWithoutHit()));
            }

        }
    }

    public static final void applyAttackMagic(final AttackInfo attack, final ISkill theSkill, final MapleCharacter player, final MapleStatEffect effect) {
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        if (attack.real) {
            player.getCheatTracker().checkAttack(attack.skill, attack.lastAttackTickCount);
        }
        /* 確認攻擊次數 */
        int last = effect.getAttackCount() > effect.getBulletCount() ? effect.getAttackCount() : effect.getBulletCount();
        if (attack.hits > last) {
            if (player.hasGmLevel(1)) {
                player.dropMessage("攻擊次數異常攻擊次數 " + attack.hits + " 服務端判斷正常攻擊次數 " + last + " 技能ID " + attack.skill);
            } else {
                //player.ban(player.getName() + "技能攻擊次數異常", true, true, false);
                //player.getClient().getSession().close();
                String reason = "使用違法程式練功";
                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為" + reason + "而被管理員永久停權。"));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") 攻擊次數異常已自動封鎖。 玩家攻擊次數 " + attack.hits + " 服務端判斷正常攻擊次數 " + last + " 技能ID " + attack.skill));
                FileoutputUtil.logToFile("logs/Hack/Ban/技能攻擊次數.txt", "\r\n" + FileoutputUtil.NowTime() + "玩家: " + player.getName() + "(" + player.getLevel() + ") 技能代碼: " + attack.skill + " 技能等級: " + player.getSkillLevel(attack.skill) + " 攻擊次數 : " + attack.hits + " 正常攻擊次數 :" + last);
                //return;
            }
        }
        /* 確認是否超過打怪數量*/
        int CheckCount = effect.getMobCount();
        if (attack.targets > CheckCount) {
            if (player.hasGmLevel(1)) {
                player.dropMessage("打怪數量異常,技能代碼: " + attack.skill + " 封包怪物量 : " + attack.targets + " 服務端怪物量 :" + CheckCount);
            } else {
                FileoutputUtil.logToFile("logs/Hack/Ban/打怪數量異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家: " + player.getName() + "(" + player.getLevel() + ") 技能代碼: " + attack.skill + " 技能等級: " + player.getSkillLevel(effect.getSourceId()) + " 封包怪物量 : " + attack.targets + " 服務端怪物量 :" + CheckCount);
                //player.ban(player.getName() + "打怪數量異常", true, true, false);
                //player.getClient().getSession().close();
                String reason = "使用違法程式練功";
                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為" + reason + "而被管理員永久停權。"));
                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") " + "攻擊怪物數量異常。 " + "封包怪物量 " + attack.targets + " 服務端怪物量 " + CheckCount + " 技能ID " + attack.skill));
                //return;
            }
        }
        if (attack.hits > 0 && attack.targets > 0) {
            if (!player.getStat().checkEquipDurabilitys(player, -1)) { //i guess this is how it works ?
                player.dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
                return;
            } //lol
        }
        if (GameConstants.isMulungSkill(attack.skill)) {
            if (player.getMapId() / 10000 != 92502) {
                //AutobanManager.getInstance().autoban(player.getClient(), "Using Mu Lung dojo skill out of dojo maps.");
                return;
            } else {
                player.mulungEnergyModify(false);
            }
        }
        if (GameConstants.isPyramidSkill(attack.skill)) {
            if (player.getMapId() / 1000000 != 926) {
                //AutobanManager.getInstance().autoban(player.getClient(), "Using Pyramid skill outside of pyramid maps.");
                return;
            } else if (player.getPyramidSubway() == null || !player.getPyramidSubway().onSkillUse(player)) {
                return;
            }
        }
        final PlayerStats stats = player.getStat();
        double maxDamagePerHit;
        if (attack.skill == 1000 || attack.skill == 10001000 || attack.skill == 20001000 || attack.skill == 20011000 || attack.skill == 30001000) {
            maxDamagePerHit = 40;
        } else if (GameConstants.isPyramidSkill(attack.skill)) {
            maxDamagePerHit = 1;
        } else {
            final double v75 = (effect.getMatk() * 0.058);
            maxDamagePerHit = stats.getTotalMagic() * (stats.getInt() * 0.5 + (v75 * v75) + effect.getMatk() * 3.3) / 100;
        }
        maxDamagePerHit *= 1.04; // Avoid any errors for now

        final Element element = player.getBuffedValue(MapleBuffStat.ELEMENT_RESET) != null ? Element.NEUTRAL : theSkill.getElement();

        double MaxDamagePerHit = 0;
        int totDamageToOneMonster, totDamage = 0, fixeddmg;
        byte overallAttackCount;
        boolean Tempest;
        MapleMonsterStats monsterstats;
        int CriticalDamage = stats.passive_sharpeye_percent();
        final ISkill eaterSkill = SkillFactory.getSkill(GameConstants.getMPEaterForJob(player.getJob()));
        final int eaterLevel = player.getSkillLevel(eaterSkill);

        final MapleMap map = player.getMap();

        for (final AttackPair oned : attack.allDamage) {
            final MapleMonster monster = map.getMonsterByOid(oned.objectid);

            if (monster != null) {
                Tempest = monster.getStatusSourceID(MonsterStatus.FREEZE) == 21120006 && !monster.getStats().isBoss();
                totDamageToOneMonster = 0;
                monsterstats = monster.getStats();
                fixeddmg = monsterstats.getFixedDamage();
                MaxDamagePerHit = calculateMaxMagicDamagePerHit(player, theSkill, monster, monsterstats, stats, element, CriticalDamage, maxDamagePerHit);
                overallAttackCount = 0;
                Integer eachd;
                for (Pair<Integer, Boolean> eachde : oned.attack) {
                    eachd = eachde.left;
                    overallAttackCount++;
                    /* 確認是否超過預計傷害*/
                    if (!GameConstants.isElseSkill(attack.skill)) {
                        if (GameConstants.Novice_Skill(attack.skill)) {//新手技能
                            if (eachd > 40) {
                                FileoutputUtil.logToFile("logs/Hack/Ban/傷害異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家<" + player.getLevel() + ">: " + player.getName() + " 怪物 " + monster.getId() + " 地圖: " + player.getMapId() + " 技能代碼: " + attack.skill + " 最高傷害: 40 本次傷害 :" + eachd + " 預計傷害: " + (int) maxDamagePerHit + "是否為BOSS: " + monster.getStats().isBoss());
                                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為傷害異常而被管理員永久停權。"));
                                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") " + "傷害異常。 " + "最高傷害 40 本次傷害 " + eachd + " 技能ID " + attack.skill));
                                //player.ban(player.getName() + "傷害異常", true, true, false);
                                //player.getClient().getSession().close();
                                //return;
                            }
                        }
                        int atk = 20000000;
                        if ((!GameConstants.isAran(player.getJob()) && player.getLevel() > 10)) {
                            boolean ban = false;
                            if (player.getLevel() <= 20) {
                                atk = 1000;
                            } else if (player.getLevel() <= 30) {
                                atk = 2500;
                            } else if (player.getLevel() <= 60) {
                                atk = 8000;
                            }

                            if (attack.skill == 1001004 || attack.skill == 11001002 || attack.skill == 5111002 || attack.skill == 15101005) {
                                atk *= 2;
                            }

                            if (eachd >= atk && eachd > Math.ceil(maxDamagePerHit * 1.2)) {
                                ban = true;
                            }
                            if (eachd == monster.getMobMaxHp()) {
                                ban = false;
                            }
                            if (player.hasGmLevel(1)) {
                                ban = false;
                            }
                            if (ban) {
                                boolean apple = false;
                                if (player.getBuffSource(MapleBuffStat.WATK) == 2022179 || player.getBuffSource(MapleBuffStat.MATK) == 2022179 || player.getBuffSource(MapleBuffStat.WDEF) == 2022179) {
                                    apple = true;
                                }
                                FileoutputUtil.logToFile("logs/Hack/Ban/傷害異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家<" + player.getLevel() + ">: " + player.getName() + " 怪物 " + monster.getId() + " 地圖: " + player.getMapId() + " 技能代碼: " + attack.skill + " 最高傷害: " + atk + " 本次傷害 :" + eachd + " 預計傷害: " + (int) maxDamagePerHit + "是否為BOSS: " + monster.getStats().isBoss() + " 紫色蘋果: " + apple);
                                //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為傷害異常而被管理員永久停權。"));
                                World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") " + "傷害異常。 " + "最高傷害 " + atk + " 本次傷害 " + eachd + " 技能ID " + attack.skill));
                                //player.ban(player.getName() + "傷害異常", true, true, false);
                                //player.getClient().getSession().close();
                                //return;
                            }
                        }
                    }

                    /*if (fixeddmg != -1) {
                        eachd = monsterstats.getOnlyNoramlAttack() ? 0 : fixeddmg; // Magic is always not a normal attack
                    } else if (monsterstats.getOnlyNoramlAttack()) {
                        eachd = 0; // Magic is always not a normal attack
                    } else if (!player.isGM()) {
                        if (Tempest) { // Buffed with Tempest
                            if (eachd > monster.getMobMaxHp()) {
                                eachd = (int) Math.min(monster.getMobMaxHp(), Integer.MAX_VALUE);
                                player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC);
                            }
                        } else if (!monster.isBuffed(MonsterStatus.DAMAGE_IMMUNITY) && !monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY) && !monster.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) {
                            if (eachd > maxDamagePerHit) {
                                player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE, new StringBuilder().append("[傷害: ").append(eachd).append(", 預期: ").append(maxDamagePerHit).append(", 怪物: ").append(monster.getId()).append("] [職業: ").append(player.getJob()).append(", 等級: ").append(player.getLevel()).append(", 使用的技能: ").append(attack.skill).append("]").toString());
                                if (attack.real) {
                                    player.getCheatTracker().checkSameDamage(eachd.intValue(), maxDamagePerHit);
                                }
                                if (eachd > MaxDamagePerHit * 2) {
//				    System.out.println("EXCEED!!! Client damage : " + eachd + " Server : " + MaxDamagePerHit);
                                    eachd = (int) (MaxDamagePerHit * 2); // Convert to server calculated damage
                                    player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC_2, new StringBuilder().append("[傷害: ").append(eachd).append(", 預期: ").append(maxDamagePerHit).append(", 怪物: ").append(monster.getId()).append("] [職業: ").append(player.getJob()).append(", 等級: ").append(player.getLevel()).append(", 使用的技能: ").append(attack.skill).append("]").toString());
                                    if (eachd >= 10000) {
                                        if (ServerConfig.LOG_DAMAGE) {
                                            FileoutputUtil.logToFile("Logs/hack/傷害計算/魔法傷害計算修正_" + attack.skill + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家: " + player.getName() + "(" + player.getLevel() + ") 職業: " + player.getJob() + " 怪物:" + monster.getId() + " 封包傷害 :" + eachd + " 預計傷害 :" + (int) maxDamagePerHit + "是否為BOSS: " + monster.getStats().isBoss(), false, false);
                                        }
                                        player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC_2, new StringBuilder().append("[傷害: ").append(eachd).append(", 預期: ").append(maxDamagePerHit).append(", 怪物: ").append(monster.getId()).append("] [職業: ").append(player.getJob()).append(", 等級: ").append(player.getLevel()).append(", 使用的技能: ").append(attack.skill).append("]").toString());
                                        return;
                                    }
                                }
                            }
                        } else if (eachd > maxDamagePerHit) {
                            eachd = (int) (maxDamagePerHit);
                            if (eachd > MaxDamagePerHit * 2 && maxDamagePerHit != 1) {
                                if (ServerConfig.LOG_DAMAGE) {
                                    FileoutputUtil.logToFile("Logs/hack/傷害計算/魔法傷害計算修正_" + attack.skill + ".txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家: " + player.getName() + "(" + player.getLevel() + ") 職業: " + player.getJob() + " 怪物:" + monster.getId() + " 封包傷害 :" + eachd + " 預計傷害 :" + (int) maxDamagePerHit + "是否為BOSS: " + monster.getStats().isBoss(), false, false);
                                }
                            }
                        }
                    }*/
                    totDamageToOneMonster += eachd;
                }
                totDamage += totDamageToOneMonster;
                player.checkMonsterAggro(monster);

                double range = player.getPosition().distanceSq(monster.getPosition());
                double SkillRange = GameConstants.getAttackRange(player, effect, attack);
                if (player.getDebugMessage() && range > SkillRange) {
                    player.dropMessage("技能[" + attack.skill + "] 預計範圍: " + (int) SkillRange + " 實際範圍: " + (int) range);
                }
                if (range > SkillRange && !player.inBossMap()) { // 815^2 <-- the most ranged attack in the game is Flame Wheel at 815 range
                    player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, "攻擊範圍異常,技能:" + attack.skill + "(" + SkillFactory.getName(attack.skill) + ")　正常範圍:" + (int) SkillRange + " 計算範圍:" + (int) range); // , Double.toString(Math.sqrt(distance))
                    if (range > SkillRange * 2) {
                        player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER_BAN, "超大攻擊範圍,技能:" + attack.skill + "(" + SkillFactory.getName(attack.skill) + ")　怪物:" + monster.getId() + " 正常範圍:" + (int) SkillRange + " 計算範圍:" + (int) range); // , Double.toString(Math.sqrt(distance))
                    }
                    return;
                }
                if (attack.skill == SkillType.僧侶.群體治癒 && !monsterstats.getUndead()) {
                    player.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
                    FileoutputUtil.logToFile("logs/Hack/Ban/技能異常.txt", "\r\n " + FileoutputUtil.NowTime() + " 玩家<" + player.getLevel() + ">: " + player.getName() + " 怪物 " + monster.getId() + " 地圖: " + player.getMapId() + " 技能代碼: " + attack.skill + " 使用群體治癒攻擊非不死系怪物");
                   // World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[封鎖系統] " + player.getName() + " 因為技能異常而被管理員永久停權。"));
                    World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM 密語系統] " + player.getName() + " (等級 " + player.getLevel() + ") " + "技能異常。 使用群體治癒攻擊非不死系怪物 技能ID " + attack.skill));
                    player.ban(player.getName() + "修改WZ", true, true, false);
                    player.getClient().getSession().close();
                    return;
                }

                if (totDamageToOneMonster > 0) {
                    monster.damage(player, totDamageToOneMonster, true, attack.skill);
                    if (monster.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) { //test
                        player.addHP(-(7000 + Randomizer.nextInt(8000))); //this is what it seems to be?
                    }
                    // effects
                    switch (attack.skill) {
                        case SkillType.冰雷大魔導士.寒冰地獄:
                            monster.setTempEffectiveness(Element.FIRE, theSkill.getEffect(player.getSkillLevel(theSkill)).getDuration());
                            break;
                        case SkillType.火毒大魔導士.炎靈地獄:
                            monster.setTempEffectiveness(Element.ICE, theSkill.getEffect(player.getSkillLevel(theSkill)).getDuration());
                            break;
                    }
                    if (effect.getMonsterStati().size() >= 0) {
                        if (effect.makeChanceResult()) {
                            for (Map.Entry<MonsterStatus, Integer> z : effect.getMonsterStati().entrySet()) {
                                monster.applyStatus(player, new MonsterStatusEffect(z.getKey(), z.getValue(), theSkill.getId(), null, false), effect.isPoison(), effect.getDuration(), monster.getStats().isBoss(), effect);
                            }
                        }
                    }
                    if (eaterLevel > 0) {
                        eaterSkill.getEffect(eaterLevel).applyPassive(player, monster);
                    }
                }
            }
        }
        if (attack.skill != SkillType.僧侶.群體治癒) {
            effect.applyTo(player);
        }

        if (totDamage > 1) {
            final CheatTracker tracker = player.getCheatTracker();
            tracker.setAttacksWithoutHit(true);

            if (tracker.getAttacksWithoutHit() > 1000) {
                tracker.registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, Integer.toString(tracker.getAttacksWithoutHit()));
            }
        }
    }

    private static double calculateMaxMagicDamagePerHit(final MapleCharacter chr, final ISkill skill, final MapleMonster monster, final MapleMonsterStats mobstats, final PlayerStats stats, final Element elem, final Integer sharpEye, final double maxDamagePerMonster) {
        final int dLevel = Math.max(mobstats.getLevel() - chr.getLevel(), 0);
        final int Accuracy = (int) (Math.floor((stats.getTotalInt() / 10.0)) + Math.floor((stats.getTotalLuk() / 10.0)));
        final int MinAccuracy = mobstats.getEva() * (dLevel * 2 + 51) / 120;
        // FullAccuracy = Avoid * (dLevel * 2 + 51) / 50

        if (MinAccuracy > Accuracy && skill.getId() != 1000 && skill.getId() != 10001000 && skill.getId() != 20001000 && skill.getId() != 20011000 && skill.getId() != 30001000 && !GameConstants.isPyramidSkill(skill.getId())) { // miss :P or HACK :O
            return 0;
        }
        double elemMaxDamagePerMob;

        switch (monster.getEffectiveness(elem)) {
            case IMMUNE://免疫
                elemMaxDamagePerMob = 1;
                break;
            case NORMAL:
                elemMaxDamagePerMob = ElementalStaffAttackBonus(elem, maxDamagePerMonster, stats);
                break;
            case WEAK:
                elemMaxDamagePerMob = ElementalStaffAttackBonus(elem, maxDamagePerMonster * 1.5, stats);
                break;
            case STRONG:
                elemMaxDamagePerMob = ElementalStaffAttackBonus(elem, maxDamagePerMonster * 0.5, stats);
                break;
            default:
                throw new RuntimeException("Unknown enum constant");
        }
        // Calculate monster magic def
        // Min damage = (MIN before defense) - MDEF*.6
        // Max damage = (MAX before defense) - MDEF*.5
        elemMaxDamagePerMob -= mobstats.getMagicDefense() * 0.5;
        // Calculate Sharp eye bonus
        elemMaxDamagePerMob += ((double) elemMaxDamagePerMob / 100) * sharpEye;
//	if (skill.isChargeSkill()) {
//	    elemMaxDamagePerMob = (float) ((90 * ((System.currentTimeMillis() - chr.getKeyDownSkill_Time()) / 1000) + 10) * elemMaxDamagePerMob * 0.01);
//	}
//      if (skill.isChargeSkill() && chr.getKeyDownSkill_Time() == 0) {
//          return 1;
//      }
        if (skill.getId() == SkillType.狂狼勇士4.極冰暴風) {
            elemMaxDamagePerMob *= 15;
        }
        if (skill.getId() == SkillType.冰雷魔導士.冰雷合擊) {
            elemMaxDamagePerMob *= 2;
        }

        elemMaxDamagePerMob += (elemMaxDamagePerMob * (mobstats.isBoss() ? stats.bossdam_r : stats.dam_r)) / 100;
        switch (skill.getId()) {
            case SkillType.冒險之技.嫩寶丟擲術:
            case SkillType.貴族.嫩寶丟擲術:
            case SkillType.傳說.嫩寶丟擲術:
                elemMaxDamagePerMob = 40;
                break;
            case SkillType.冒險之技.法老的憤怒攻擊:
            case SkillType.貴族.法老的憤怒攻擊:
            case SkillType.傳說.法老的憤怒攻擊:
                elemMaxDamagePerMob = 1;
                break;
        }
        if (elemMaxDamagePerMob > 20000000) {
            elemMaxDamagePerMob = 20000000;
        } else if (elemMaxDamagePerMob < 0) {
            elemMaxDamagePerMob = 1;
        }
        return elemMaxDamagePerMob;
    }

    private static final double ElementalStaffAttackBonus(final Element elem, double elemMaxDamagePerMob, final PlayerStats stats) {
        switch (elem) {
            case FIRE:
                return (elemMaxDamagePerMob / 100) * stats.element_fire;
            case ICE:
                return (elemMaxDamagePerMob / 100) * stats.element_ice;
            case LIGHTING:
                return (elemMaxDamagePerMob / 100) * stats.element_light;
            case POISON:
                return (elemMaxDamagePerMob / 100) * stats.element_psn;
            default:
                return (elemMaxDamagePerMob / 100) * stats.def;
        }
    }

    private static void handlePickPocket(final MapleCharacter player, final MapleMonster mob, AttackPair oned) {
        int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET);
        final ISkill skill = SkillFactory.getSkill(4211003);
        final MapleStatEffect s = skill.getEffect(player.getSkillLevel(skill));
        for (Pair eachde : oned.attack) {
            Integer eachd = (Integer) eachde.left;
            if (s.makeChanceResult()) {
                player.getMap().spawnMesoDrop(Math.min((int) Math.max(eachd / 20000.0D * maxmeso, 1.0D), maxmeso), new Point((int) (mob.getTruePosition().getX() + Randomizer.nextInt(100) - 50.0D), (int) mob.getTruePosition().getY()), mob, player, false, (byte) 0);
            }
        }
    }

    private static double calculateMaxWeaponDamagePerHit(final MapleCharacter player, final MapleMonster monster, final AttackInfo attack, final ISkill theSkill, final MapleStatEffect attackEffect, double maximumDamageToMonster, final Integer CriticalDamagePercent) {
        if (player.getMapId() / 1000000 == 914) { // 教程地圖
            return 500000;
        }
        List<Element> elements = new ArrayList<>();
        boolean defined = false;
        if (theSkill != null) {
            elements.add(theSkill.getElement());

            if (monster.getStatusSourceID(MonsterStatus.FREEZE) == 21120006) {
                defined = true;
            }
            switch (theSkill.getId()) {
                case SkillType.神射手.光速神弩:
                case SkillType.弓箭手.斷魂箭:
                    defined = true; //傷害可超過 199999
                    break;
                case 1000:
                case 10001000:
                case 20001000:
                case 20011000:
                case 30001000:
                    maximumDamageToMonster = 40;
                    defined = true;
                    break;
                case 1020:
                case 10001020:
                case 20001020:
                case 20011020:
                case 30001020:
                    maximumDamageToMonster = 1;
                    defined = true;
                    break;
                case SkillType.神射手.必殺狙擊: // Sniping
                    maximumDamageToMonster = (monster.getStats().isBoss() ? 500000 : monster.getMobMaxHp());
                    defined = true;
                    break;
                case SkillType.聖騎士.鬼神之擊://Heavens Hammer
                    maximumDamageToMonster = (monster.getStats().isBoss() ? 500000 : monster.getHp() - 1);
                    defined = true;
                    break;
                case SkillType.神偷.楓幣炸彈: // Meso Explosion
                    maximumDamageToMonster = (monster.getStats().isBoss() ? 500000 : monster.getMobMaxHp());
                    defined = true;
                    break;
                case 1009: // Bamboo Trust
                case 10001009:
                case 20001009:
                case 20011009:
                case 30001009:
                    defined = true;
                    maximumDamageToMonster = (monster.getStats().isBoss() ? monster.getMobMaxHp() / 30 * 100 : monster.getMobMaxHp());
                    break;
                case 3211006: //Sniper Strafe
                    if (monster.getStatusSourceID(MonsterStatus.FREEZE) == 3211003) { //blizzard in effect
                        defined = true;
                        maximumDamageToMonster = monster.getHp();
                    }
                    break;
                case SkillType.拳霸.閃連殺:
                    maximumDamageToMonster *= 2.8;
                    break;
                case SkillType.英雄.究極突刺:
                case SkillType.十字軍.虎咆哮:
                case SkillType.龍騎士.龍咆哮:
                case SkillType.龍騎士.槍連擊:
                case SkillType.俠盜.迴旋斬:// BOSS
                    maximumDamageToMonster *= 3;
                    break;
                case SkillType.劍士.魔天一擊:
                case SkillType.英雄.無雙劍舞:
                case SkillType.暗影神偷.致命暗殺:
                    maximumDamageToMonster *= 2.5;
                    break;
                case SkillType.龍騎士.無雙矛:
                case SkillType.劍士.劍氣蹤橫:
                case SkillType.箭神.暴風神射: // Hurricane
                    maximumDamageToMonster *= 2;
                    break;
            }
        }
        if (MapleJob.is狂狼勇士(player.getJob())) {
            maximumDamageToMonster *= 2;
        } else if (MapleJob.is拳霸(player.getJob())) {
            maximumDamageToMonster *= 1.1;
        }
        if (player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {
            int chargeSkillId = player.getBuffSource(MapleBuffStat.WK_CHARGE);

            switch (chargeSkillId) {
                case SkillType.騎士.烈焰之劍:
                case SkillType.騎士.烈焰之棍:
                    elements.add(Element.FIRE);
                    break;
                case SkillType.騎士.寒冰之劍:
                case SkillType.騎士.寒冰之棍:
                case SkillType.狂狼勇士3.寒冰屬性:
                    elements.add(Element.ICE);
                    break;
                case SkillType.騎士.雷鳴之劍:
                case SkillType.騎士.雷鳴之棍:
                case SkillType.閃雷悍將2.雷鳴:
                    elements.add(Element.LIGHTING);
                    break;
                case SkillType.聖騎士.聖靈之劍:
                case SkillType.聖騎士.聖靈之棍:
                case SkillType.聖魂劍士3.靈魂屬性:
                    elements.add(Element.HOLY);
                    break;
                case SkillType.烈焰巫師2.自然力重置:
                    elements.clear(); //neutral
                    break;
            }
        }
        if (player.getBuffedValue(MapleBuffStat.LIGHTNING_CHARGE) != null) {
            elements.add(Element.LIGHTING);
        }
        double elementalMaxDamagePerMonster = maximumDamageToMonster;

        if (elements.size() > 0) {
            double elementalEffect;

            switch (attack.skill) {
                case SkillType.狙擊手.寒冰箭:
                case SkillType.遊俠.烈火箭: // inferno and blizzard
                    elementalEffect = attackEffect.getX() / 200.0;
                    break;
                default:
                    elementalEffect = 0.5;
                    break;
            }
            for (Element element : elements) {
                switch (monster.getEffectiveness(element)) {
                    case IMMUNE:// 免疫
                        elementalMaxDamagePerMonster = 1;
                        break;
                    case WEAK:
                        elementalMaxDamagePerMonster *= (1.0 + elementalEffect);
                        break;
                    case STRONG:
                        elementalMaxDamagePerMonster *= (1.0 - elementalEffect);
                        break;
                    default:
                        break; //normal nothing
                }
            }
        }
        // 計算怪物防禦
        final short moblevel = monster.getStats().getLevel();
        final short d = moblevel > player.getLevel() ? (short) (moblevel - player.getLevel()) : 0;
        elementalMaxDamagePerMonster = elementalMaxDamagePerMonster * (1 - 0.01 * d) - monster.getStats().getPhysicalDefense() * 0.5;

        // Calculate passive bonuses + Sharp Eye
        elementalMaxDamagePerMonster += ((double) elementalMaxDamagePerMonster / 100.0) * CriticalDamagePercent;

//	if (theSkill.isChargeSkill()) {
//	    elementalMaxDamagePerMonster = (double) (90 * (System.currentTimeMillis() - player.getKeyDownSkill_Time()) / 2000 + 10) * elementalMaxDamagePerMonster * 0.01;
//	}
        if (theSkill != null && theSkill.isChargeSkill() && player.getKeyDownSkill_Time() == 0 && theSkill.getId() != 4111005) {
            return 0;
        }
        final MapleStatEffect homing = player.getStatForBuff(MapleBuffStat.HOMING_BEACON);
        if (homing != null && player.getLinkMid() == monster.getObjectId() && homing.getSourceId() == 5220011) { //bullseye
            elementalMaxDamagePerMonster += (elementalMaxDamagePerMonster * homing.getX());
        }
        final PlayerStats stat = player.getStat();
        elementalMaxDamagePerMonster += (elementalMaxDamagePerMonster * (monster.getStats().isBoss() ? stat.bossdam_r * 2 : stat.dam_r)) / 100.0;
        switch (monster.getId()) {
            case 1110101://黑木妖
                elementalMaxDamagePerMonster *= 2;
                break;
        }
        if (player.getDebugMessage()) {
            player.dropMessage("[傷害計算]屬性傷害：" + (int) Math.ceil(elementalMaxDamagePerMonster) + " BOSS傷害：" + (int) Math.ceil(((monster.getStats().isBoss()) ? player.getStat().bossdam_r : player.getStat().dam_r) - 100) + "%");
        }
        if (elementalMaxDamagePerMonster > 20000000) {
            if (!defined) {
                elementalMaxDamagePerMonster = 20000000;
            }
        } else if (elementalMaxDamagePerMonster < 0) {
            elementalMaxDamagePerMonster = 1;
        }
        return elementalMaxDamagePerMonster;
    }

    public static final AttackInfo DivideAttack(final AttackInfo attack, final int rate) {
        attack.real = false;
        if (rate <= 1) {
            return attack; //lol
        }
        for (AttackPair p : attack.allDamage) {
            if (p.attack != null) {
                for (Pair<Integer, Boolean> eachd : p.attack) {
                    eachd.left /= rate; //too ex.
                }
            }
        }
        return attack;
    }

    public static final AttackInfo Modify_AttackCrit(final AttackInfo attack, final MapleCharacter chr, final int type) {

        final int criticalRate = chr.getStat().passive_sharpeye_rate();

        final boolean shadow = (type == 2 && chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null)
                || (type == 1 && chr.getBuffedValue(MapleBuffStat.MIRROR_IMAGE) != null);

        if (attack.skill != SkillType.神偷.楓幣炸彈
                && attack.skill != SkillType.狙擊手.寒冰箭
                && attack.skill != SkillType.暗殺者.楓幣攻擊
                && (criticalRate > 0 || attack.skill == SkillType.暗影神偷.致命暗殺 || attack.skill == SkillType.神射手.必殺狙擊)) { //blizz + shadow meso + m.e no crits

            for (AttackPair attackPair : attack.allDamage) {
                if (attackPair.attack != null) {
                    int hit = 0;
                    final int midAtt = attackPair.attack.size() / 2;
                    final List<Pair<Integer, Boolean>> eachd_copy = new ArrayList<>(attackPair.attack);
                    for (Pair<Integer, Boolean> eachd : attackPair.attack) {
                        hit++;
                        if (!eachd.right) {
                            if (attack.skill == SkillType.暗影神偷.致命暗殺) { //assassinate never crit first 3, always crit last
                                eachd.right = (hit == 4 && Randomizer.nextInt(100) < 90);
                            } else if (attack.skill == SkillType.神射手.必殺狙擊 || eachd.left > 500000) { //snipe always crit
                                eachd.right = true;
                            } else if (shadow && hit > midAtt) { //shadowpartner copies second half to first half
                                eachd.right = eachd_copy.get(hit - 1 - midAtt).right;
                            } else {
                                //rough calculation
                                eachd.right = (Randomizer.nextInt(100)/*chr.CRand().CRand32__Random_ForMonster() % 100*/) < criticalRate;
                            }
                            eachd_copy.get(hit - 1).right = eachd.right;
                            //System.out.println("CRITICAL RATE: " + CriticalRate + ", passive rate: " + chr.getStat().passive_sharpeye_rate() + ", critical: " + eachd.right);
                        }
                    }
                }
            }
        }
        return attack;
    }

    public static final AttackInfo parseDmgMa(final LittleEndianAccessor lea) {
        //System.out.println(lea.toString());
        final AttackInfo ret = new AttackInfo();

        lea.skip(1);
        lea.skip(8);
        ret.tbyte = lea.readByte();
        //System.out.println("TBYTE: " + tbyte);
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        lea.skip(8); //?
        ret.skill = lea.readInt();
        lea.skip(12); // ORDER [4] bytes on v.79, [4] bytes on v.80, [1] byte on v.82
        switch (ret.skill) {
            case 2121001: // Big Bang
            case 2221001:
            case 2321001:
            case 22121000: //breath
            case 22151001:
                ret.charge = lea.readInt();
                break;
            default:
                ret.charge = -1;
                break;
        }
        lea.skip(1);
        ret.unk = 0;
        ret.display = lea.readByte(); // Always zero?
        ret.animation = lea.readByte();
        lea.skip(1); // Weapon subclass
        ret.speed = lea.readByte(); // Confirmed
        ret.lastAttackTickCount = lea.readInt(); // Ticks
//        lea.skip(4); //0

        int oid, damage;
        List<Pair<Integer, Boolean>> allDamageNumbers;
        ret.allDamage = new ArrayList<>();

        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            lea.skip(14); // [1] Always 6?, [3] unk, [4] Pos1, [4] Pos2, [2] seems to change randomly for some attack

            allDamageNumbers = new ArrayList<>();

            for (int j = 0; j < ret.hits; j++) {
                damage = lea.readInt();
                allDamageNumbers.add(new Pair<>(damage, false));
            }
            lea.skip(4); // CRC of monster [Wz Editing]
            ret.allDamage.add(new AttackPair(oid, allDamageNumbers));
        }
        ret.position = lea.readPos();

        return ret;
    }

    public static final AttackInfo parseDmgM(final LittleEndianAccessor lea) {
        //System.out.println(lea.toString());
        final AttackInfo ret = new AttackInfo();

        lea.skip(1);
        lea.skip(8);
        ret.tbyte = lea.readByte();
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        lea.skip(8);
        ret.skill = lea.readInt();
        lea.skip(12); // ORDER [4] bytes on v.79, [4] bytes on v.80, [1] byte on v.82

        switch (ret.skill) {
            case SkillType.打手.狂暴衝擊: // Corkscrew
            case SkillType.閃雷悍將2.狂暴衝擊: // Cygnus corkscrew
            case SkillType.槍手.炸彈投擲: // Gernard
            case SkillType.暗夜行者3.毒炸彈: // Poison bomb
                ret.charge = lea.readInt();
                break;
            default:
                ret.charge = 0;
                break;
        }

        ret.unk = lea.readByte();
        ret.display = lea.readByte(); // Always zero?
        ret.animation = lea.readByte();
        lea.skip(1); // Weapon class
        ret.speed = lea.readByte(); // Confirmed
        ret.lastAttackTickCount = lea.readInt(); // Ticks
        ret.allDamage = new ArrayList<>();

        if (ret.skill == SkillType.神偷.楓幣炸彈) {
            return parseExplosionAttack(lea, ret);
        }
        int oid, damage;
        List<Pair<Integer, Boolean>> allDamageNumbers;

        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            lea.skip(14); // [1] Always 6?, [3] unk, [4] Pos1, [4] Pos2, [2] seems to change randomly for some attack
            allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < ret.hits; j++) {
                damage = lea.readInt();
                allDamageNumbers.add(new Pair<>(damage, false));
            }
            lea.skip(4); // CRC of monster [Wz Editing]
            ret.allDamage.add(new AttackPair(oid, allDamageNumbers));
        }
        ret.position = lea.readPos();
        if (ret.skill == 14111006) {
            ret.positionxy = lea.readPos();
        }
        return ret;
    }

    public static final AttackInfo parseDmgR(final LittleEndianAccessor lea) {

        final AttackInfo ret = new AttackInfo();

        lea.skip(1);
        lea.skip(8);
        ret.tbyte = lea.readByte();
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        lea.skip(8);
        ret.skill = lea.readInt();

        lea.skip(12); // ORDER [4] bytes on v.79, [4] bytes on v.80, [1] byte on v.82

        switch (ret.skill) {
            case SkillType.箭神.暴風神射: // Hurricane
            case SkillType.神射手.光速神弩: // Pierce
            case SkillType.槍神.瞬迅雷: // Rapidfire
            case SkillType.破風使者3.暴風神射: // Cygnus Hurricane
                lea.skip(4); // extra 4 bytes
                break;
        }

        ret.charge = -1;
        ret.unk = lea.readByte();
        ret.display = lea.readByte(); // Always zero?
        ret.animation = lea.readByte();
        lea.skip(1); // Weapon class
        ret.speed = lea.readByte(); // Confirmed
        ret.lastAttackTickCount = lea.readInt(); // Ticks
        ret.slot = (byte) lea.readShort();
        ret.csstar = (byte) lea.readShort();
        ret.AOE = lea.readByte(); // is AOE or not, TT/ Avenger = 41, Showdown = 0

        int damage, oid;
        List<Pair<Integer, Boolean>> allDamageNumbers;
        ret.allDamage = new ArrayList<>();

        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            lea.skip(14); // [1] Always 6?, [3] unk, [4] Pos1, [4] Pos2, [2] seems to change randomly for some attack
            allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < ret.hits; j++) {
                damage = lea.readInt();
                boolean add = allDamageNumbers.add(new Pair<>(damage, false));
            }
            lea.skip(4); // CRC of monster [Wz Editing]
            ret.allDamage.add(new AttackPair(oid, allDamageNumbers));
        }
        lea.skip(4);
        ret.position = lea.readPos();
        return ret;
    }

    public static final AttackInfo parseExplosionAttack(final LittleEndianAccessor lea, final AttackInfo ret) {

        if (ret.hits == 0) {
            lea.skip(4);
            byte bullets = lea.readByte();
            for (int j = 0; j < bullets; j++) {
                ret.allDamage.add(new AttackPair(lea.readInt(), null));
                lea.skip(1);
            }
            lea.skip(2); // 8F 02
            return ret;
        }

        for (int i = 0; i < ret.targets; i++) {
            int oid = lea.readInt();
            lea.skip(12);
            byte bullets = lea.readByte();
            List<Pair<Integer, Boolean>> allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < bullets; j++) {
                allDamageNumbers.add(new Pair<>(lea.readInt(), false)); //m.e. never crits
            }
            ret.allDamage.add(new AttackPair(oid, allDamageNumbers));
            lea.skip(4); // C3 8F 41 94, 51 04 5B 01
        }
        lea.skip(4);
        byte bullets = lea.readByte();

        for (int j = 0; j < bullets; j++) {
            ret.allDamage.add(new AttackPair(lea.readInt(), null));
            lea.skip(1);
        }
        lea.skip(2); // 8F 02/ 63 02
        return ret;
    }
}
