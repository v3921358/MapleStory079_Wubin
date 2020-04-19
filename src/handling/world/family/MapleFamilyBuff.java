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
package handling.world.family;

import client.MapleBuffStat;
import client.MapleCharacter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.MapleStatEffect.CancelEffectAction;
import server.Timer.BuffTimer;
import tools.MaplePacketCreator;
import tools.Pair;

public class MapleFamilyBuff {

    //todo; read from somewhere
    private static final int event = 2; //numevents
    // 0=tele, 1=summ, 2=drop, 3=exp, 4=both
    // questrecords used for time: 190000 to 190010
    private static final int[] type = {0, 1, 2, 3, 4, 2, 3, 2, 3, 2, 3};
    private static final int[] duration = {0, 0, 15, 15, 30, 15, 15, 30, 30, 30, 30};
    private static final int[] effect = {0, 0, 150, 150, 200, 200, 200, 200, 200, 200, 200};
    private static final int[] rep = {0, 0, 300, 500, 700, 800, 1000, 1200, 1500, 2000, 2500, 4000, 5000}; //70% of normal in gms O_O
    private static final String[] name = {
        "立刻移動至家族成員",
        "立刻召喚家族成員"
    /*,
     "我的掉寶率1.5倍(15 分鐘)",
     "我的經驗值1.5倍(15分鐘)",
     "家族成員的團結(30分鐘)",
     "我的掉寶率 2倍(15分鐘)",
     "我的經驗值2倍(15分鐘)",
     "我的掉寶率2倍(30分鐘)",
     "我的經驗值2倍(30分鐘)",
     "我的隊伍掉寶率2倍",
     "我的隊伍經驗值2倍"
     */
    };

    private static final String[] desc = {"[對象] 自己\n[效果] 移動到想要的上線家族成員所在地圖。",
        "[對象] 1個家族對象\n[效果] 召喚指定的上線家族成員到自己所在的地圖。"
    /*,
     "[對象] 自己\n[時間] 15 分鐘.\n[效果] 獵捕怪物的掉寶率提升 #c1.5倍#\n* 如果有使用其他加倍這效果將無效.",
     "[對象] 自己\n[時間] 15 分鐘.\n[效果] 獵捕怪物的經驗值提升 #c1.5倍#\n* 如果有使用其他加倍這效果將無效.",
     "[對象] 至少有6個成員以上在線上\n[時間] 30 分鐘　\n[效果] 獵捕怪物的掉寶率和經驗值提升 #c2倍#\n* 如果有使用其他加倍這效果將無效.",
     "[對象] 自己\n[時間] 15 分鐘.\n[效果] 獵捕怪物的掉寶率提升 #c2倍#\n* 如果有使用其他加倍這效果將無效.",
     "[對象] 自己\n[時間] 15 分鐘.\n[效果] 獵捕怪物的經驗值提升 #c2倍#\n* 如果有使用其他加倍這效果將無效.",
     "[對象] 自己\n[時間] 30 分鐘.\n[效果] 獵捕怪物的掉寶率提升 #c2倍#\n* 如果有使用其他加倍這效果將無效.",
     "[對象] 自己\n[時間] 30 分鐘.\n[效果] 獵捕怪物的經驗值提升 #c2倍#\n* 如果有使用其他加倍這效果將無效.",
     "[對象] 我的隊伍\n[時間] 30 分鐘.\n[效果] 同一張地圖內的所屬隊伍成員掉寶率提升 #c2倍#\n* 如果有使用其他加倍這效果將無效.",
     "[對象] 我的隊伍\n[時間] 30 分鐘.\n[效果] 同一張地圖內的所屬隊伍成員經驗值提升 #c2倍#\n* 如果有使用其他加倍這效果將無效."*/
    };

    private final static List<MapleFamilyBuffEntry> buffEntries;

    static {
        buffEntries = new ArrayList<>();
        for (int i = 0; i < event; i++) { //count = 1, questid = 190000+i
            buffEntries.add(new MapleFamilyBuffEntry(i, name[i], desc[i], 1, rep[i], type[i], 190000 + i, duration[i], effect[i]));
        }
    }

    public static List<MapleFamilyBuffEntry> getBuffEntry() {
        return buffEntries;
    }

    public static MapleFamilyBuffEntry getBuffEntry(int i) {
        return buffEntries.get(i);
    }

    public static class MapleFamilyBuffEntry {

        public String name, desc;
        public int count, rep, type, index, questID, duration, effect;
        public List<Pair<MapleBuffStat, Integer>> effects;

        public MapleFamilyBuffEntry(int index, String name, String desc, int count, int rep, int type, int questID, int duration, int effect) {
            this.name = name;
            this.desc = desc;
            this.count = count;
            this.rep = rep;
            this.type = type;
            this.questID = questID;
            this.index = index;
            this.duration = duration;
            this.effect = effect;
            this.effects = getEffects();
        }

        public int getEffectId() {
            switch (type) {
                case 2: //drop
                    return 2022694;
                case 3: //exp
                    return 2450018;
            }
            return 2022332; //custom
        }

        public final List<Pair<MapleBuffStat, Integer>> getEffects() {
            //custom
            List<Pair<MapleBuffStat, Integer>> ret = new ArrayList<>();
            switch (type) {
                case 2: //drop
                    ret.add(new Pair<>(MapleBuffStat.DROP_RATE, effect));
                    ret.add(new Pair<>(MapleBuffStat.MESO_RATE, effect));
                    break;
                case 3: //exp
                    ret.add(new Pair<>(MapleBuffStat.EXPRATE, effect));
                    break;
                case 4: //both
                    ret.add(new Pair<>(MapleBuffStat.EXPRATE, effect));
                    ret.add(new Pair<>(MapleBuffStat.DROP_RATE, effect));
                    ret.add(new Pair<>(MapleBuffStat.MESO_RATE, effect));
                    break;
            }
            return ret;
        }

        public void applyTo(MapleCharacter chr) {
            chr.getClient().sendPacket(MaplePacketCreator.giveBuff(-getEffectId(), duration * 60000, effects, null));
            final MapleStatEffect eff = MapleItemInformationProvider.getInstance().getItemEffect(getEffectId());
            chr.cancelEffect(eff, true, -1, effects);
            final long starttime = System.currentTimeMillis();
            final CancelEffectAction cancelAction = new CancelEffectAction(chr, eff, starttime);
            final ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, ((starttime + (duration * 60000)) - starttime));
            chr.registerEffect(eff, starttime, schedule, effects, false, duration, chr.getId());
        }
    }
}
