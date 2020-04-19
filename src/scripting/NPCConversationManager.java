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
package scripting;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import client.inventory.Equip;
import client.ISkill;
import client.inventory.IItem;
import client.MapleCharacter;
import constants.GameConstants;
import client.inventory.ItemFlag;
import client.MapleClient;
import client.MapleJob;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.SkillFactory;
import client.SkillEntry;
import client.MapleStat;
import client.inventory.Item;
import client.inventory.ItemLoader;
import client.status.MonsterStatus;
import database.DBConPool;
import server.MapleCarnivalParty;
import server.Randomizer;
import server.MapleInventoryManipulator;
import server.MapleShopFactory;
import server.MapleSquad;
import server.maps.MapleMap;
import server.maps.Event_DojoAgent;
import server.maps.AramiaFireWorks;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packet.PlayerShopPacket;
import server.MapleItemInformationProvider;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.channel.MapleGuildRanking.JzRankingInfo;
import handling.channel.handler.InterServerHandler;
import handling.channel.handler.InventoryHandler;
import handling.login.LoginServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.guild.MapleGuild;
import server.MapleCarnivalChallenge;
import java.util.HashMap;
import handling.world.guild.MapleGuildAlliance;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Random;
import javax.script.Invocable;
import static server.MapleCarnivalChallenge.getJobNameById;
import server.MapleStatEffect;
import server.MerchItemPackage;
import server.SpeedRunner;
import server.Start;
import server.maps.SpeedRunType;
import server.StructPotentialItem;
import server.Timer;
import server.Timer.CloneTimer;
import server.gashapon.Gashapon;
import server.gashapon.GashaponFactory;
import server.life.Element;
import static server.life.Element.DARKNESS;
import static server.life.Element.FIRE;
import static server.life.Element.ICE;
import static server.life.Element.LIGHTING;
import static server.life.Element.POISON;
import server.life.ElementalEffectiveness;
import static server.life.ElementalEffectiveness.STRONG;
import static server.life.ElementalEffectiveness.WEAK;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import server.maps.Event_PyramidSubway;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.shops.HiredMerchant;
import server.活动神秘商人;
import server.活动野外通缉;
import tools.FilePrinter;
import tools.FileoutputUtil;
import static tools.FileoutputUtil.CurrentReadable_Date;
import tools.SearchGenerator;
import tools.StringUtil;
import tools.packet.UIPacket;

public class NPCConversationManager extends AbstractPlayerInteraction {

    protected MapleClient c;
    private final int npc, questid, mode;
    protected String script;
    private String getText;
    private final byte type; // -1 = NPC, 0 = start quest, 1 = end quest
    private byte lastMsg = -1;
    public boolean pendingDisposal = false;
    private final Invocable iv;

    public NPCConversationManager(MapleClient c, int npc, int questid, int mode, String npcscript, byte type, Invocable iv) {
        super(c);
        this.c = c;
        this.npc = npc;
        this.questid = questid;
        this.mode = mode;
        this.type = type;
        this.iv = iv;
        this.script = npcscript;
    }

    public Invocable getIv() {
        return iv;
    }

    public int getMode() {
        return mode;
    }

    public int getNpc() {
        return npc;
    }

    public int getQuest() {
        return questid;
    }

    public String getScript() {
        return script;
    }

    public byte getType() {
        return type;
    }

    public void safeDispose() {
        pendingDisposal = true;
    }

    public void dispose() {
        NPCScriptManager.getInstance().dispose(c);
    }

    public void askMapSelection(final String sel) {
        if (lastMsg > -1) {
            return;
        }
        c.sendPacket(MaplePacketCreator.getMapSelection(npc, sel));
        lastMsg = 0xD;
    }

    public void sendNext(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { //sendNext will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", (byte) 0));
        lastMsg = 0;
    }

    public void sendNextS(String text, byte type) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", type));
        lastMsg = 0;
    }

    public void sendPrev(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", (byte) 0));
        lastMsg = 0;
    }

    public void sendPrevS(String text, byte type) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", type));
        lastMsg = 0;
    }

    public void sendNextPrev(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", (byte) 0));
        lastMsg = 0;
    }

    public void PlayerToNpc(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text, byte type) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", type));
        lastMsg = 0;

    }

    public void sendOk(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
        lastMsg = 0;
    }

    public void sendOkS(String text, byte type) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", type));
        lastMsg = 0;
    }

    public void sendYesNo(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 1, text, "", (byte) 0));
        lastMsg = 1;
    }

    public void sendYesNoS(String text, byte type) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 1, text, "", type));
        lastMsg = 1;
    }

    public void sendAcceptDecline(String text) {
        askAcceptDecline(text);
    }

    public void sendAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text);
    }

    public void askAcceptDecline(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0xB, text, "", (byte) 0));
        lastMsg = 0xB;
    }

    public void askAcceptDeclineNoESC(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0xC, text, "", (byte) 0));
        lastMsg = 0xC;
    }

    public void askAvatar(String text, int... args) {
        if (lastMsg > -1) {
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalkStyle(npc, text, args));
        lastMsg = 7;
    }

    public void sendSimple(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendNext(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 4, text, "", (byte) 0));
        lastMsg = 4;
    }

    public void sendSimpleS(String text, byte type) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendNextS(text, type);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 4, text, "", (byte) type));
        lastMsg = 4;
    }

    public void sendStyle(String text, int styles[]) {
        if (lastMsg > -1) {
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalkStyle(npc, text, styles));
        lastMsg = 7;
    }

    public void sendStyle(String text, int a, int styles[]) {
        if (lastMsg > -1) {
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalkStyle(npc, text, a, styles));
        lastMsg = 7;
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalkNum(npc, text, def, min, max));
        lastMsg = 3;
    }

    public void sendGetText(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalkText(npc, text));
        lastMsg = 2;
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return getText;
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor((byte) color);
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public int setRandomAvatar(int ticket, int... args_all) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);

        int args = args_all[Randomizer.nextInt(args_all.length)];
        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public int setAvatar(int ticket, int args) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);

        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public void sendStorage() {
        if (getPlayer().hasBlockedInventory2(true)) { //hack
            c.getPlayer().dropMessage(1, "系统错误，请联系管理员。");
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }

        if (!World.isShutDown) {
            if (!World.isShopShutDown) {
                c.getPlayer().setConversation(4);
                c.getPlayer().getStorage().sendStorage(c, npc);
            } else {
                c.getPlayer().dropMessage(1, "目前不能使用仓库。");
                c.sendPacket(MaplePacketCreator.enableActions());
            }
        } else {
            c.getPlayer().dropMessage(1, "目前不能使用仓库。");
            c.sendPacket(MaplePacketCreator.enableActions());
        }
    }

    public void openShop(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(c);
    }

    public void openShopNPC(int shopid) {
        MapleShopFactory.getInstance().getShop(shopid).sendShop(c, npc);
    }

    public int gainGachaponItem(int id, int quantity) {
        return gainGachaponItem(id, quantity, c.getPlayer().getMap().getStreetName() + " - " + c.getPlayer().getMap().getMapName());
    }

    public int gainGachaponItem(int id, int quantity, final String msg) {
        try {
            if (!MapleItemInformationProvider.getInstance().itemExists(id)) {
                return -1;
            }
            final IItem item = MapleInventoryManipulator.addbyId_Gachapon(c, id, (short) quantity);

            if (item == null) {
                return -1;
            }
            final byte rareness = GameConstants.gachaponRareItem(item.getItemId());
            if (rareness == 1) {
                if (c.getPlayer().getMapId() == 910000000) {
                    World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[自由市场]", " : 恭喜玩家 " + c.getPlayer().getName() + " 在" + msg + "获得！", item, rareness));
                } else {
                    World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[隐藏地图-豆豆屋]", " : 恭喜玩家 " + c.getPlayer().getName() + " 在" + msg + "获得！", item, rareness));
                }
                //World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[" + msg + "] " + c.getPlayer().getName(), " : 被他抽到了，大家恭喜他吧！", item, rareness));
            } else if (rareness == 2) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[" + msg + "] " + c.getPlayer().getName(), " : 被他成功抽到了，大家恭喜他吧！", item, rareness));
            } else if (rareness > 2) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[" + msg + "] " + c.getPlayer().getName(), " : 被他成功抽到了，大家恭喜他吧！", item, rareness));
            }
            return item.getItemId();
        } catch (Exception e) {
        }
        return -1;
    }

    public int gainGachaponItemTime(int id, int quantity, long period) {
        return gainGachaponItemTime(id, quantity, c.getPlayer().getMap().getStreetName() + " - " + c.getPlayer().getMap().getMapName(), period);
    }

    public int gainGachaponItemTime(int id, int quantity, final String msg, long period) {
        try {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(id)) {
                return -1;
            }
            final IItem item = ii.isCash(id) ? MapleInventoryManipulator.addbyId_GachaponTime(c, id, (short) quantity, period) : MapleInventoryManipulator.addbyId_Gachapon(c, id, (short) quantity);

            if (item == null) {
                return -1;
            }
            final byte rareness = GameConstants.gachaponRareItem(item.getItemId());
            if (rareness == 1) {
                if (c.getPlayer().getMapId() == 910000000) {
                    World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[自由市场]", " : 恭喜玩家 " + c.getPlayer().getName() + " 在" + msg + "获得！", item, rareness));
                } else {
                    World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[隐藏地图-豆豆屋]", " : 恭喜玩家 " + c.getPlayer().getName() + " 在" + msg + "获得！", item, rareness));
                }
                //World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[" + msg + "] " + c.getPlayer().getName(), " : 被他抽到了，大家恭喜他吧！", item, rareness));
            } else if (rareness == 2) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[" + msg + "] " + c.getPlayer().getName(), " : 被他成功抽到了，大家恭喜他吧！", item, rareness));
            } else if (rareness > 2) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[" + msg + "] " + c.getPlayer().getName(), " : 被他成功抽到了，大家恭喜他吧！", item, rareness));
            }
            return item.getItemId();
        } catch (Exception e) {
        }
        return -1;
    }

    public void changeJob(int job) {
        c.getPlayer().changeJob(job);
    }

    public void startQuest(int id) {
        MapleQuest.getInstance(id).start(getPlayer(), npc);
    }

    public void completeQuest(int id) {
        MapleQuest.getInstance(id).complete(getPlayer(), npc);
    }

    public void forfeitQuest(int id) {
        MapleQuest.getInstance(id).forfeit(getPlayer());
    }

    public void forceStartQuest() {
        MapleQuest.getInstance(questid).forceStart(getPlayer(), getNpc(), null);
    }

    @Override
    public void forceStartQuest(int id) {
        MapleQuest.getInstance(id).forceStart(getPlayer(), getNpc(), null);
    }

    public void forceStartQuest(String customData) {
        MapleQuest.getInstance(questid).forceStart(getPlayer(), getNpc(), customData);
    }

    public void forceCompleteQuest() {
        MapleQuest.getInstance(questid).forceComplete(getPlayer(), getNpc());
    }

    @Override
    public void forceCompleteQuest(final int id) {
        MapleQuest.getInstance(id).forceComplete(getPlayer(), getNpc());
    }

    public String getQuestCustomData() {
        return c.getPlayer().getQuestNAdd(MapleQuest.getInstance(questid)).getCustomData();
    }

    public void setQuestCustomData(String customData) {
        getPlayer().getQuestNAdd(MapleQuest.getInstance(questid)).setCustomData(customData);
    }

    public int getMeso() {
        return getPlayer().getMeso();
    }

    public int 判断金币() {
        return getPlayer().getMeso();
    }

    public void gainAp(final int amount) {
        c.getPlayer().gainAp((short) amount);
    }

    public void expandInventory(byte type, int amt) {
        c.getPlayer().expandInventory(type, amt);
    }

    public void unequipEverything() {
        MapleInventory equipped = getPlayer().getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<Short> ids = new LinkedList<>();
        for (IItem item : equipped.list()) {
            ids.add(item.getPosition());
        }
        for (short id : ids) {
            MapleInventoryManipulator.unequip(getC(), id, equip.getNextFreeSlot());
        }
    }

    public final void clearSkills() {
        Map<ISkill, SkillEntry> skills = getPlayer().getSkills();
        for (Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
            getPlayer().changeSkillLevel(skill.getKey(), (byte) 0, (byte) 0);
        }
    }

    public boolean hasSkill(int skillid) {
        ISkill theSkill = SkillFactory.getSkill(skillid);
        if (theSkill != null) {
            return c.getPlayer().getSkillLevel(theSkill) > 0;
        }
        return false;
    }

    public void showEffect(boolean broadcast, String effect) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect(effect));
        } else {
            c.sendPacket(MaplePacketCreator.showEffect(effect));
        }
    }

    public void playSound(boolean broadcast, String sound) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound(sound));
        } else {
            c.sendPacket(MaplePacketCreator.playSound(sound));
        }
    }

    public void environmentChange(boolean broadcast, String env) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(env, 2));
        } else {
            c.sendPacket(MaplePacketCreator.environmentChange(env, 2));
        }
    }

    public void updateBuddyCapacity(int capacity) {
        c.getPlayer().setBuddyCapacity((byte) capacity);
    }

    public int getBuddyCapacity() {
        return c.getPlayer().getBuddyCapacity();
    }

    public int partyMembersInMap() {
        int inMap = 0;
        for (MapleCharacter char2 : getPlayer().getMap().getCharactersThreadsafe()) {
            if (char2.getParty() == getPlayer().getParty()) {
                inMap++;
            }
        }
        return inMap;
    }

    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<>(); // creates an empty array full of shit..
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                if (ch != null) { // double check <3
                    chars.add(ch);
                }
            }
        }
        return chars;
    }

    public void warpPartyWithExp(int mapId, int exp) {
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
            }
        }
    }

    public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
                curChar.gainMeso(meso, true);
            }
        }
    }

    public MapleSquad getSquad(String type) {
        return c.getChannelServer().getMapleSquad(type);
    }

    public int getSquadAvailability(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        }
        return squad.getStatus();
    }

    public boolean registerSquad(String type, int minutes, String startText) {
        if (c.getChannelServer().getMapleSquad(type) == null) {
            final MapleSquad squad = new MapleSquad(c.getChannel(), type, c.getPlayer(), minutes * 60 * 1000, startText);
            final boolean ret = c.getChannelServer().addMapleSquad(squad, type);
            if (ret) {
                final MapleMap map = c.getPlayer().getMap();

                map.broadcastMessage(MaplePacketCreator.getClock(minutes * 60));
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + startText));
            } else {
                squad.clear();
            }
            return ret;
        }
        return false;
    }

    public boolean getSquadList(String type, byte type_) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return false;
        }
        if (type_ == 0 || type_ == 3) { // Normal viewing
            sendNext(squad.getSquadMemberString(type_));
        } else if (type_ == 1) { // Squad Leader banning, Check out banned participant
            sendSimple(squad.getSquadMemberString(type_));
        } else if (type_ == 2) {
            if (squad.getBannedMemberSize() > 0) {
                sendSimple(squad.getSquadMemberString(type_));
            } else {
                sendNext(squad.getSquadMemberString(type_));
            }
        }
        return true;

    }

    public byte isSquadLeader(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else if (squad.getLeader() != null && squad.getLeader().getId() == c.getPlayer().getId()) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean reAdd(String eim, String squad) {
        EventInstanceManager eimz = getDisconnected(eim);
        MapleSquad squadz = getSquad(squad);
        if (eimz != null && squadz != null) {
            squadz.reAddMember(getPlayer());
            eimz.registerPlayer(getPlayer());
            return true;
        }
        return false;
    }

    public void banMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(pos);
        }
    }

    public void acceptMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.acceptMember(pos);
        }
    }

    public String getReadableMillis(long startMillis, long endMillis) {
        return StringUtil.getReadableMillis(startMillis, endMillis);
    }

    public int addMember(String type, boolean join) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            return squad.addMember(c.getPlayer(), join);
        }
        return -1;
    }

    public byte isSquadMember(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else if (squad.getMembers().contains(c.getPlayer().getName())) {
            return 1;
        } else if (squad.isBanned(c.getPlayer())) {
            return 2;
        } else {
            return 0;
        }
    }

    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    public void genericGuildMessage(int code) {
        c.sendPacket(MaplePacketCreator.genericGuildMessage((byte) code));
    }

    public void disbandGuild() {
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0 || c.getPlayer().getGuildRank() != 1) {
            return;
        }
        World.Guild.disbandGuild(gid);
    }

    public void increaseGuildCapacity() {
        if (c.getPlayer().getMeso() < 250000) {
            c.sendPacket(MaplePacketCreator.serverNotice(1, "You do not have enough mesos."));
            return;
        }
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0) {
            return;
        }
        World.Guild.increaseGuildCapacity(gid);
        c.getPlayer().gainMeso(-250000, true, false, true);
    }

    public void displayGuildRanks() {
        c.sendPacket(MaplePacketCreator.showGuildRanks(npc, MapleGuildRanking.getInstance().getGuildRank()));
    }

    public void showlvl() {
        c.sendPacket(MaplePacketCreator.showlevelRanks(npc, MapleGuildRanking.getInstance().getLevelRank()));
    }

    public void showmeso() {
        c.sendPacket(MaplePacketCreator.showmesoRanks(npc, MapleGuildRanking.getInstance().getMesoRank()));
    }

    public String showJzMeso(int mapid) {
        String a = "目前排名：\r\n\r\n";
        List<JzRankingInfo> all = MapleGuildRanking.getInstance().getJzRank(mapid);
        for (MapleGuildRanking.JzRankingInfo info : all) {
            a += " #b" + getPlayer().getCharacterNameById(info.getId()) + "#k : " + info.getMeso() + "金币\r\n";

        }
        a += "\r\n我不能透露现在捐赠的人数有谁，";
        a += "\r\n这些记录每周六凌晨23：59截止每周一凌晨00：00清空。";
        return a;
    }

    public boolean removePlayerFromInstance() {
        if (c.getPlayer().getEventInstance() != null) {
            c.getPlayer().getEventInstance().removePlayer(c.getPlayer());
            return true;
        }
        return false;
    }

    public boolean isPlayerInstance() {
        return c.getPlayer().getEventInstance() != null;
    }

    public void changeStat(byte slot, int type, short amount) {
        Equip sel = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        switch (type) {
            case 0:
                sel.setStr(amount);
                break;
            case 1:
                sel.setDex(amount);
                break;
            case 2:
                sel.setInt(amount);
                break;
            case 3:
                sel.setLuk(amount);
                break;
            case 4:
                sel.setHp(amount);
                break;
            case 5:
                sel.setMp(amount);
                break;
            case 6:
                sel.setWatk(amount);
                break;
            case 7:
                sel.setMatk(amount);
                break;
            case 8:
                sel.setWdef(amount);
                break;
            case 9:
                sel.setMdef(amount);
                break;
            case 10:
                sel.setAcc(amount);
                break;
            case 11:
                sel.setAvoid(amount);
                break;
            case 12:
                sel.setHands(amount);
                break;
            case 13:
                sel.setSpeed(amount);
                break;
            case 14:
                sel.setJump(amount);
                break;
            case 15:
                sel.setUpgradeSlots((byte) amount);
                break;
            case 16:
                sel.setViciousHammer((byte) amount);
                break;
            case 17:
                sel.setLevel((byte) amount);
                break;
            case 18:
                sel.setEnhance((byte) amount);
                break;
            case 19:
                sel.setPotential1(amount);
                break;
            case 20:
                sel.setPotential2(amount);
                break;
            case 21:
                sel.setPotential3(amount);
                break;
            case 22:
                sel.setOwner(getText());
                break;
            default:
                break;
        }
        c.getPlayer().equipChanged();
    }

    public void cleardrops() {
        MapleMonsterInformationProvider.getInstance().clearDrops();
    }

    public void killAllMonsters() {
        MapleMap map = c.getPlayer().getMap();
        double range = Double.POSITIVE_INFINITY;
        MapleMonster mob;
        for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
            mob = (MapleMonster) monstermo;
            if (mob.getStats().isBoss()) {
                map.killMonster(mob, c.getPlayer(), false, false, (byte) 1);
            }
        }
        /*int mapid = c.getPlayer().getMapId();
         MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
         map.killAllMonsters(true); // No drop. */
    }

    public void giveMerchantMesos() {
        long mesos = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = (PreparedStatement) con.prepareStatement("SELECT mesos FROM hiredmerchants WHERE merchantid = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
            } else {
                mesos = rs.getLong("mesos");
            }
            rs.close();
            ps.close();

            ps = (PreparedStatement) con.prepareStatement("UPDATE hiredmerchants SET mesos = 0 WHERE merchantid = ?");
            ps.setInt(1, getPlayer().getId());
            ps.executeUpdate();
            ps.close();

        } catch (SQLException ex) {
            System.err.println("Error gaining mesos in hired merchant" + ex);
            FileoutputUtil.outError("logs/数据库异常.txt", ex);
        }
        c.getPlayer().gainMeso((int) mesos, true);
    }

    public void dc() {
        MapleCharacter victim = getChannelServer().getPlayerStorage().getCharacterByName(getPlayer().getName());
        victim.getClient().getSession().close();
        victim.getClient().disconnect(true, false);
    }

    public long getMerchantMesos() {
        long mesos = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection(); PreparedStatement ps = (PreparedStatement) con.prepareStatement("SELECT mesos FROM hiredmerchants WHERE merchantid = ?")) {
            ps.setInt(1, getPlayer().getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    rs.close();
                    ps.close();
                } else {
                    mesos = rs.getLong("mesos");
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error gaining mesos in hired merchant" + ex);
            FileoutputUtil.outError("logs/数据库异常.txt", ex);
        }
        return mesos;
    }

    public void openDuey() {
        c.getPlayer().setConversation(2);
        c.sendPacket(MaplePacketCreator.sendDuey((byte) 9, null));
    }

    public void openMerchantItemStore() {
        if (!World.isShutDown) {
            c.getPlayer().setConversation(3);
            c.sendPacket(PlayerShopPacket.merchItemStore((byte) 0x22));
        } else {
            c.getPlayer().dropMessage(1, "目前不能使用雇佣商店领取。");
            c.sendPacket(MaplePacketCreator.enableActions());
        }
    }

    public void sendRepairWindow() {
        c.sendPacket(MaplePacketCreator.sendRepairWindow(npc));
    }

    public final int getDojoPoints() {
        return c.getPlayer().getDojo();
    }

    public void setDojoPoints(int point) {
        c.getPlayer().setDojo(c.getPlayer().getDojo() + point);
    }

    public final int getDojoRecord() {
        return c.getPlayer().getDojoRecord();
    }

    public void setDojoRecord(final boolean reset) {
        c.getPlayer().setDojoRecord(reset);
    }

    public boolean start_DojoAgent(final boolean dojo, final boolean party) {
        if (dojo) {
            return Event_DojoAgent.warpStartDojo(c.getPlayer(), party);
        }
        return Event_DojoAgent.warpStartAgent(c.getPlayer(), party);
    }

    public boolean start_PyramidSubway(final int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpStartPyramid(c.getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpStartSubway(c.getPlayer());
    }

    public boolean bonus_PyramidSubway(final int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpBonusPyramid(c.getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpBonusSubway(c.getPlayer());
    }

    public final short getKegs() {
        return AramiaFireWorks.getInstance().getKegsPercentage();
    }

    public void giveKegs(final int kegs) {
        AramiaFireWorks.getInstance().giveKegs(c.getPlayer(), kegs);
    }

    public final short getSunshines() {
        return AramiaFireWorks.getInstance().getSunsPercentage();
    }

    public void addSunshines(final int kegs) {
        AramiaFireWorks.getInstance().giveSuns(c.getPlayer(), kegs);
    }

    public final short getDecorations() {
        return AramiaFireWorks.getInstance().getDecsPercentage();
    }

    public void addDecorations(final int kegs) {
        try {
            AramiaFireWorks.getInstance().giveDecs(c.getPlayer(), kegs);
        } catch (Exception e) {
        }
    }

    public final MapleInventory getInventory(int type) {
        return c.getPlayer().getInventory(MapleInventoryType.getByType((byte) type));
    }

    public final MapleCarnivalParty getCarnivalParty() {
        return c.getPlayer().getCarnivalParty();
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return c.getPlayer().getNextCarnivalRequest();
    }

    public final MapleCarnivalChallenge getCarnivalChallenge(MapleCharacter chr) {
        return new MapleCarnivalChallenge(chr);
    }

    public void maxStats() {
        Map<MapleStat, Integer> statup = new EnumMap<>(MapleStat.class);
        c.getPlayer().getStat().setStr((short) 32767);
        c.getPlayer().getStat().setDex((short) 32767);
        c.getPlayer().getStat().setInt((short) 32767);
        c.getPlayer().getStat().setLuk((short) 32767);

        c.getPlayer().getStat().setMaxHp((short) 30000);
        c.getPlayer().getStat().setMaxMp((short) 30000);
        c.getPlayer().getStat().setHp((short) 30000);
        c.getPlayer().getStat().setMp((short) 30000);

        statup.put(MapleStat.STR, 32767);
        statup.put(MapleStat.DEX, 32767);
        statup.put(MapleStat.LUK, 32767);
        statup.put(MapleStat.INT, 32767);
        statup.put(MapleStat.HP, 30000);
        statup.put(MapleStat.MAXHP, 30000);
        statup.put(MapleStat.MP, 30000);
        statup.put(MapleStat.MAXMP, 30000);

        c.sendPacket(MaplePacketCreator.updatePlayerStats(statup, c.getPlayer()));
    }

    public Pair<String, Map<Integer, String>> getSpeedRun(String typ) {
        final SpeedRunType stype = SpeedRunType.valueOf(typ);
        if (SpeedRunner.getInstance().getSpeedRunData(stype) != null) {
            return SpeedRunner.getInstance().getSpeedRunData(stype);
        }
        return new Pair<>("", new HashMap<>());
    }

    public boolean getSR(Pair<String, Map<Integer, String>> ma, int sel) {
        if (ma.getRight().get(sel) == null || ma.getRight().get(sel).length() <= 0) {
            dispose();
            return false;
        }
        sendOk(ma.getRight().get(sel));
        return true;
    }

    public Equip getEquip(int itemid) {
        return (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
    }

    public void setExpiration(Object statsSel, long expire) {
        if (statsSel instanceof Equip) {
            ((Equip) statsSel).setExpiration(System.currentTimeMillis() + (expire * 24 * 60 * 60 * 1000));
        }
    }

    public void setLock(Object statsSel) {
        if (statsSel instanceof Equip) {
            Equip eq = (Equip) statsSel;
            if (eq.getExpiration() == -1) {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.LOCK.getValue()));
            } else {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
            }
        }
    }

    public boolean addFromDrop(Object statsSel) {
        if (statsSel instanceof IItem) {
            final IItem it = (IItem) statsSel;
            return MapleInventoryManipulator.checkSpace(getClient(), it.getItemId(), it.getQuantity(), it.getOwner()) && MapleInventoryManipulator.addFromDrop(getClient(), it, false);
        }
        return false;
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type) {
        return replaceItem(slot, invType, statsSel, offset, type, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type, boolean takeSlot) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        IItem item = getPlayer().getInventory(inv).getItem((byte) slot);
        if (item == null || statsSel instanceof IItem) {
            item = (IItem) statsSel;
        }
        if (offset > 0) {
            if (inv != MapleInventoryType.EQUIP) {
                return false;
            }
            Equip eq = (Equip) item;
            if (takeSlot) {
                if (eq.getUpgradeSlots() < 1) {
                    return false;
                } else {
                    eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() - 1));
                }
            }
            if (type.equalsIgnoreCase("Slots")) {
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + offset));
            } else if (type.equalsIgnoreCase("Level")) {
                eq.setLevel((byte) (eq.getLevel() + offset));
            } else if (type.equalsIgnoreCase("Hammer")) {
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("STR")) {
                eq.setStr((short) (eq.getStr() + offset));
            } else if (type.equalsIgnoreCase("DEX")) {
                eq.setDex((short) (eq.getDex() + offset));
            } else if (type.equalsIgnoreCase("INT")) {
                eq.setInt((short) (eq.getInt() + offset));
            } else if (type.equalsIgnoreCase("LUK")) {
                eq.setLuk((short) (eq.getLuk() + offset));
            } else if (type.equalsIgnoreCase("HP")) {
                eq.setHp((short) (eq.getHp() + offset));
            } else if (type.equalsIgnoreCase("MP")) {
                eq.setMp((short) (eq.getMp() + offset));
            } else if (type.equalsIgnoreCase("WATK")) {
                eq.setWatk((short) (eq.getWatk() + offset));
            } else if (type.equalsIgnoreCase("MATK")) {
                eq.setMatk((short) (eq.getMatk() + offset));
            } else if (type.equalsIgnoreCase("WDEF")) {
                eq.setWdef((short) (eq.getWdef() + offset));
            } else if (type.equalsIgnoreCase("MDEF")) {
                eq.setMdef((short) (eq.getMdef() + offset));
            } else if (type.equalsIgnoreCase("ACC")) {
                eq.setAcc((short) (eq.getAcc() + offset));
            } else if (type.equalsIgnoreCase("Avoid")) {
                eq.setAvoid((short) (eq.getAvoid() + offset));
            } else if (type.equalsIgnoreCase("Hands")) {
                eq.setHands((short) (eq.getHands() + offset));
            } else if (type.equalsIgnoreCase("Speed")) {
                eq.setSpeed((short) (eq.getSpeed() + offset));
            } else if (type.equalsIgnoreCase("Jump")) {
                eq.setJump((short) (eq.getJump() + offset));
            } else if (type.equalsIgnoreCase("ItemEXP")) {
                eq.setItemEXP(eq.getItemEXP() + offset);
            } else if (type.equalsIgnoreCase("Expiration")) {
                eq.setExpiration((long) (eq.getExpiration() + offset));
            } else if (type.equalsIgnoreCase("Flag")) {
                eq.setFlag((byte) (eq.getFlag() + offset));
            }
            if (eq.getExpiration() == -1) {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.LOCK.getValue()));
            } else {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
            }
            item = eq.copy();
        }
        MapleInventoryManipulator.removeFromSlot(getClient(), inv, (short) slot, item.getQuantity(), false);
        return MapleInventoryManipulator.addFromDrop(getClient(), item, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int upgradeSlots) {
        return replaceItem(slot, invType, statsSel, upgradeSlots, "Slots");
    }

    public boolean isCash(final int itemId) {
        return MapleItemInformationProvider.getInstance().isCash(itemId);
    }

    public void buffGuild(final int buff, final int duration, final String msg) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.getItemEffect(buff) != null && getPlayer().getGuildId() > 0) {
            final MapleStatEffect mse = ii.getItemEffect(buff);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharactersThreadSafe()) {
                    if (chr.getGuildId() == getPlayer().getGuildId()) {
                        mse.applyTo(chr, chr, true, null, duration);
                        chr.dropMessage(5, "Your guild has gotten a " + msg + " buff.");
                    }
                }
            }
        }
    }

    public boolean createAlliance(String alliancename) {
        MapleParty pt = c.getPlayer().getParty();
        MapleCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(pt.getMemberByIndex(1).getId());
        if (otherChar == null || otherChar.getId() == c.getPlayer().getId()) {
            return false;
        }
        try {
            return World.Alliance.createAlliance(alliancename, c.getPlayer().getId(), otherChar.getId(), c.getPlayer().getGuildId(), otherChar.getGuildId());
        } catch (Exception re) {
            return false;
        }
    }

    public boolean addCapacityToAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId() && World.Alliance.changeAllianceCapacity(gs.getAllianceId())) {
                    gainMeso(-MapleGuildAlliance.CHANGE_CAPACITY_COST);
                    return true;
                }
            }
        } catch (Exception re) {
        }
        return false;
    }

    public boolean disbandAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId() && World.Alliance.disbandAlliance(gs.getAllianceId())) {
                    return true;
                }
            }
        } catch (Exception re) {
        }
        return false;
    }

    public byte getLastMsg() {
        return lastMsg;
    }

    public final void setLastMsg(final byte last) {
        this.lastMsg = last;
    }

    public void setPartyBossLog(String bossid) {
        MapleParty party = getPlayer().getParty();
        for (MaplePartyCharacter pc : party.getMembers()) {
            MapleCharacter chr = World.getStorage(this.getChannelNumber()).getCharacterById(pc.getId());
            if (chr != null) {
                chr.setBossLog(bossid);
            }
        }
    }

    public final void maxAllSkills() {
        for (ISkill skil : SkillFactory.getAllSkills()) {
            if (GameConstants.isApplicableSkill(skil.getId())) { //no db/additionals/resistance skills
                teachSkill(skil.getId(), skil.getMaxLevel(), skil.getMaxLevel());
            }
        }
    }

    public final void resetStats(int str, int dex, int z, int luk) {
        c.getPlayer().resetStats(str, dex, z, luk);
    }

    public final boolean dropItem(int slot, int invType, int quantity) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        return MapleInventoryManipulator.drop(c, inv, (short) slot, (short) quantity, true);
    }

    public final List<Integer> getAllPotentialInfo() {
        return new ArrayList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().keySet());
    }

    public final String getPotentialInfo(final int id) {
        final List<StructPotentialItem> potInfo = MapleItemInformationProvider.getInstance().getPotentialInfo(id);
        final StringBuilder builder = new StringBuilder("#b#ePOTENTIAL INFO FOR ID: ");
        builder.append(id);
        builder.append("#n#k\r\n\r\n");
        int minLevel = 1, maxLevel = 10;
        for (StructPotentialItem item : potInfo) {
            builder.append("#eLevels ");
            builder.append(minLevel);
            builder.append("~");
            builder.append(maxLevel);
            builder.append(": #n");
            builder.append(item.toString());
            minLevel += 10;
            maxLevel += 10;
            builder.append("\r\n");
        }
        return builder.toString();
    }

    public final void sendRPS() {
        c.sendPacket(MaplePacketCreator.getRPSMode((byte) 8, -1, -1, -1));
    }

    public final void setQuestRecord(Object ch, final int questid, final String data) {
        ((MapleCharacter) ch).getQuestNAdd(MapleQuest.getInstance(questid)).setCustomData(data);
    }

    public final void doWeddingEffect(final Object ch) {
        final MapleCharacter chr = (MapleCharacter) ch;
        getMap().broadcastMessage(MaplePacketCreator.yellowChat(chr.getName() + ", 妳愿意承认接纳 " + getPlayer().getName() + " 做你的丈夫，诚实遵照上帝的旨命，和她生活在一起，无论在什么环境意顺服他、爱惜他、安慰他、尊重他、保护他，以至白头到老？？"));
        CloneTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                if (chr == null || getPlayer() == null) {
                    warpMap(680000500, 0);
                } else {
                    getMap().broadcastMessage(MaplePacketCreator.yellowChat(getPlayer().getName() + ", 你愿意承认接纳 " + chr.getName() + " 做你的妻子，诚实遵照上帝的旨命，和她生活在一起，无论在什么环境愿意终生养她、爱惜她、安慰她、尊重她、保护她，以至白头到老？？"));
                }
            }
        }, 10000);
        CloneTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                if (chr == null || getPlayer() == null) {
                    if (getPlayer() != null) {
                        setQuestRecord(getPlayer(), 160001, "3");
                        setQuestRecord(getPlayer(), 160002, "0");
                    } else if (chr != null) {
                        setQuestRecord(chr, 160001, "3");
                        setQuestRecord(chr, 160002, "0");
                    }
                    warpMap(680000500, 0);
                } else {
                    setQuestRecord(getPlayer(), 160001, "2");
                    setQuestRecord(chr, 160001, "2");
                    sendNPCText(getPlayer().getName() + " 和 " + chr.getName() + "， 我希望你们两个能在此时此刻永远爱着对方！", 9201002);
                    getMap().startExtendedMapEffect("那么现在请新娘亲吻 " + getPlayer().getName() + "！", 5120006);
                    if (chr.getGuildId() > 0) {
                        World.Guild.guildPacket(chr.getGuildId(), MaplePacketCreator.sendMarriage(false, chr.getName()));
                    }
                    if (chr.getFamilyId() > 0) {
                        World.Family.familyPacket(chr.getFamilyId(), MaplePacketCreator.sendMarriage(true, chr.getName()), chr.getId());
                    }
                    if (getPlayer().getGuildId() > 0) {
                        World.Guild.guildPacket(getPlayer().getGuildId(), MaplePacketCreator.sendMarriage(false, getPlayer().getName()));
                    }
                    if (getPlayer().getFamilyId() > 0) {
                        World.Family.familyPacket(getPlayer().getFamilyId(), MaplePacketCreator.sendMarriage(true, chr.getName()), getPlayer().getId());
                    }
                }
            }
        }, 20000); //10 sec 10 sec
    }

    public void 開啟小鋼珠(int type) {
        c.sendPacket(MaplePacketCreator.openBeans(getPlayer().getBeans(), type));
    }

    public void worldMessage(String text) {
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, text));
    }

    public int getBeans() {
        return getClient().getPlayer().getBeans();
    }

    public void warpBack(int mid, final int retmap, final int time) { //時間秒數

        MapleMap warpMap = c.getChannelServer().getMapFactory().getMap(mid);
        c.getPlayer().changeMap(warpMap, warpMap.getPortal(0));
        c.sendPacket(MaplePacketCreator.getClock(time));
        Timer.EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                MapleMap warpMap = c.getChannelServer().getMapFactory().getMap(retmap);
                if (c.getPlayer() != null) {
                    c.sendPacket(MaplePacketCreator.stopClock());
                    c.getPlayer().changeMap(warpMap, warpMap.getPortal(0));
                    c.getPlayer().dropMessage(6, "已经到达目的地了!");
                }
            }
        }, 1000 * time); //設定時間, (1 秒 = 1000)
    }

    public void ChangeName(String name) {
        getPlayer().setName(name);
        save();
        getPlayer().fakeRelog();
    }

    public String searchData(int type, String search) {
        return SearchGenerator.searchData(type, search);
    }

    public int[] getSearchData(int type, String search) {
        Map<Integer, String> data = SearchGenerator.getSearchData(type, search);
        if (data.isEmpty()) {
            return null;
        }
        int[] searches = new int[data.size()];
        int i = 0;
        for (int key : data.keySet()) {
            searches[i] = key;
            i++;
        }
        return searches;
    }

    public boolean foundData(int type, String search) {
        return SearchGenerator.foundData(type, search);
    }

    public boolean ReceiveMedal() {
        int acid = getPlayer().getAccountID();
        int id = getPlayer().getId();
        String name = getPlayer().getName();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int item = 1142475;
        if (!getPlayer().canHold(item)) {
            return false;
        } else if (getPlayer().haveItem(item)) {
            return false;
        }

        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM RCmedals WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (!rs.first()) {// 角色不存在於勳章表單
                return false;
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("Update RCmedals set amount = ? Where id = ?");
            ps.setInt(1, 0);
            ps.setInt(2, id);
            ps.execute();
            ps.close();
        } catch (Exception ex) {
            FilePrinter.printError("NPCConversationManager.txt", ex, "ReceiveMedal(" + name + ")");
            FileoutputUtil.outError("logs/数据库异常.txt", ex);
        }
        IItem toDrop = ii.randomizeStats((Equip) ii.getEquipById(item));
        toDrop.setGMLog(getPlayer().getName() + " 领取勋章");
        MapleInventoryManipulator.addbyItem(c, toDrop);
        FileoutputUtil.logToFile("logs/Data/NPC领取勋章.txt", "\r\n " + FileoutputUtil.NowTime() + " IP: " + c.getSession().remoteAddress().toString().split(":")[0] + " 账号: " + c.getAccountName() + " 玩家: " + c.getPlayer().getName() + " 领取了勋章");
        return true;
    }

    public String ShowJobRank(int type) {
        StringBuilder sb = new StringBuilder();
        List<MapleGuildRanking.JobRankingInfo> Ranking = MapleGuildRanking.getInstance().getJobRank(type);
        if (Ranking != null) {
            int num = 0;
            for (MapleGuildRanking.JobRankingInfo info : Ranking) {
                num++;
                sb.append("#n#e#k排名:#r ");
                sb.append(num);
                sb.append("\r\n#n#e#k玩家名字:#d ");
                sb.append(StringUtil.getRightPaddedStr(info.getName(), ' ', 13));
                sb.append("\r\n#n#e#k等级:#e#r ");
                sb.append(StringUtil.getRightPaddedStr(String.valueOf(info.getLevel()), ' ', 3));
                sb.append("\r\n#n#e#k职业:#e#b ");
                sb.append(MapleJob.getName(MapleJob.getById(info.getJob())));
                sb.append("\r\n#n#e#k力量:#e#d ");
                sb.append(StringUtil.getRightPaddedStr(String.valueOf(info.getStr()), ' ', 4));
                sb.append("\r\n#n#e#k敏捷:#e#d ");
                sb.append(StringUtil.getRightPaddedStr(String.valueOf(info.getDex()), ' ', 4));
                sb.append("\r\n#n#e#k智力:#e#d ");
                sb.append(StringUtil.getRightPaddedStr(String.valueOf(info.getInt()), ' ', 4));
                sb.append("\r\n#n#e#k运气:#e#d ");
                sb.append(StringUtil.getRightPaddedStr(String.valueOf(info.getLuk()), ' ', 4));
                sb.append("\r\n");
                sb.append("#n#k======================================================\r\n");
            }
        } else {
            sb.append("#r查询无任何结果哦");
        }
        return sb.toString();
    }

    public static boolean hairExists(int hair) {
        return MapleItemInformationProvider.hairList.containsKey(hair);
    }

    public int[] getCanHair(int[] hairs) {
        List<Integer> canHair = new ArrayList();
        List<Integer> cantHair = new ArrayList();
        for (int hair : hairs) {
            if (hairExists(hair)) {
                canHair.add(hair);
            } else {
                cantHair.add(hair);
            }
        }
        if (cantHair.size() > 0 && c.getPlayer().isAdmin()) {
            StringBuilder sb = new StringBuilder("正在读取的发型里有");
            sb.append(cantHair.size()).append("个发型客户端不支持显示，已经被清除：");
            for (int i = 0; i < cantHair.size(); i++) {
                sb.append(cantHair.get(i));
                if (i < cantHair.size() - 1) {
                    sb.append(",");
                }
            }
            playerMessage(sb.toString());
        }
        int[] getHair = new int[canHair.size()];
        for (int i = 0; i < canHair.size(); i++) {
            getHair[i] = canHair.get(i);
        }
        return getHair;
    }

    public static boolean faceExists(int face) {
        return MapleItemInformationProvider.faceLists.containsKey(face);
    }

    public int[] getCanFace(int[] faces) {
        List<Integer> canFace = new ArrayList();
        List<Integer> cantFace = new ArrayList();
        for (int face : faces) {
            if (faceExists(face)) {
                canFace.add(face);
            } else {
                cantFace.add(face);
            }
        }
        if (cantFace.size() > 0 && c.getPlayer().isAdmin()) {
            StringBuilder sb = new StringBuilder("正在读取的脸型里有");
            sb.append(cantFace.size()).append("个脸型客户端不支持显示，已经被清除：");
            for (int i = 0; i < cantFace.size(); i++) {
                sb.append(cantFace.get(i));
                if (i < cantFace.size() - 1) {
                    sb.append(",");
                }
            }
            playerMessage(sb.toString());
        }
        int[] getFace = new int[canFace.size()];
        for (int i = 0; i < canFace.size(); i++) {
            getFace[i] = canFace.get(i);
        }
        return getFace;
    }

    public String checkDrop(int mobId) {
        final List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
        if (ranks != null && ranks.size() > 0) {
            int num = 0, itemId = 0, ch = 0;
            MonsterDropEntry de;
            StringBuilder name = new StringBuilder();
            MapleMonster onemob = MapleLifeFactory.getMonster(mobId);
            name.append("#e#d冒险岛怪物详细信息预览：#n#k\r\n\r\n");
            name.append("#d怪物名称 : #b#o" + mobId + "##k\r\n");
            name.append("#d怪物等级 : #b" + onemob.getLevel() + "#k\r\n");
            name.append("#d怪物类型 : #b" + (onemob.getStats().isBoss() ? "Boss怪物" : "普通怪物") + "#k\r\n");
            name.append("#d物理防御 : #b" + (onemob.getStats().getPhysicalDefense()) + "#k\r\n");
            name.append("#d魔法防御 : #b" + (onemob.getStats().getMagicDefense()) + "#k\r\n");
            name.append("#d最大血量 : #b" + (onemob.getMobMaxHp()) + "#k\r\n");
            name.append("--------------------------------------\r\n\r\n");
            name.append("#e#d怪物属性信息：#n#k\r\n\r\n");

            name.append("#d光明 : #b" + ((onemob.getStats().getEffectiveness(Element.HOLY)) == ElementalEffectiveness.IMMUNE ? "免疫" : (onemob.getStats().getEffectiveness(Element.HOLY)) == ElementalEffectiveness.STRONG ? "抗性" : (onemob.getStats().getEffectiveness(Element.HOLY)) == ElementalEffectiveness.WEAK ? "弱点" : "正常") + "#k  ");
            name.append("#d黑暗 : #b" + ((onemob.getStats().getEffectiveness(Element.DARKNESS)) == ElementalEffectiveness.IMMUNE ? "免疫" : (onemob.getStats().getEffectiveness(Element.DARKNESS)) == ElementalEffectiveness.STRONG ? "抗性" : (onemob.getStats().getEffectiveness(Element.DARKNESS)) == ElementalEffectiveness.WEAK ? "弱点" : "正常") + "#k  ");
            name.append("#d雷电 : #b" + ((onemob.getStats().getEffectiveness(Element.LIGHTING)) == ElementalEffectiveness.IMMUNE ? "免疫" : (onemob.getStats().getEffectiveness(Element.LIGHTING)) == ElementalEffectiveness.STRONG ? "抗性" : (onemob.getStats().getEffectiveness(Element.LIGHTING)) == ElementalEffectiveness.WEAK ? "弱点" : "正常") + "#k\r\n");
            name.append("#d冰冻 : #b" + ((onemob.getStats().getEffectiveness(Element.ICE)) == ElementalEffectiveness.IMMUNE ? "免疫" : (onemob.getStats().getEffectiveness(Element.ICE)) == ElementalEffectiveness.STRONG ? "抗性" : (onemob.getStats().getEffectiveness(Element.ICE)) == ElementalEffectiveness.WEAK ? "弱点" : "正常") + "#k  ");
            name.append("#d毒素 : #b" + ((onemob.getStats().getEffectiveness(Element.POISON)) == ElementalEffectiveness.IMMUNE ? "免疫" : (onemob.getStats().getEffectiveness(Element.POISON)) == ElementalEffectiveness.STRONG ? "抗性" : (onemob.getStats().getEffectiveness(Element.POISON)) == ElementalEffectiveness.WEAK ? "弱点" : "正常") + "#k  ");
            name.append("#d火焰 : #b" + ((onemob.getStats().getEffectiveness(Element.FIRE)) == ElementalEffectiveness.IMMUNE ? "免疫" : (onemob.getStats().getEffectiveness(Element.FIRE)) == ElementalEffectiveness.STRONG ? "抗性" : (onemob.getStats().getEffectiveness(Element.FIRE)) == ElementalEffectiveness.WEAK ? "弱点" : "正常") + "#k\r\n");
            name.append("--------------------------------------\r\n\r\n");
            for (int i = 0; i < ranks.size(); i++) {
                de = ranks.get(i);
                if (de.chance > 0 && (de.questid <= 0 || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                    itemId = de.itemId;
                    if (num == 0) {

                        name.append("#e#d怪物爆物信息：#n#k\r\n\r\n");
                    }
                    String namez = "#z" + itemId + "#";
                    if (itemId == 0) { //meso
                        itemId = 4031041; //display sack of cash
                        namez = (de.Minimum * getClient().getChannelServer().getMesoRate()) + " 到 " + (de.Maximum * getClient().getChannelServer().getMesoRate()) + " 金币";
                    }
                    ch = de.chance * getClient().getChannelServer().getDropRate();
                    name.append((num + 1) + ") #v" + itemId + "#" + namez + (getPlayer().isGM() ? " - #r" + (Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "% 爆率。 #k" : "") + (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("需要接受任务 " + MapleQuest.getInstance(de.questid).getName() + "") : "") + "\r\n");
                    num++;
                }
            }
            if (name.length() > 0) {
                return name.toString();
            }

        }
        return "没有当前怪物的爆物信息。";
    }

    public String checkDrop(MapleCharacter chr, int mobId, boolean GM) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
        if (ranks != null && ranks.size() > 0) {
            int num = 0, itemId = 0, ch = 0;
            MonsterDropEntry de;
            StringBuilder name = new StringBuilder();
            StringBuilder error = new StringBuilder();
            name.append("【#r#o" + mobId + "##k】爆物物品查询列表:#b" + "\r\n");
            for (int i = 0; i < ranks.size(); i++) {
                de = ranks.get(i);
                if (de.chance > 0 && (de.questid <= 0 || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                    itemId = de.itemId;
                    /*   if (num == 0) {
                        name.append("【#r#o"+ mobId + "##k】掉寶數據列表:#b" +"\r\n");
                    }*/
                    String namez = "#z" + itemId + "#";
                    if (itemId == 0) { //meso
                        itemId = 4031041; //display sack of cash
                        namez = (de.Minimum * getClient().getChannelServer().getMesoRate()) + " to " + (de.Maximum * getClient().getChannelServer().getMesoRate()) + " #b楓幣#l#k";
                    } else if (itemId != 0 && ii.itemExists(itemId)) {
                        ch = de.chance * getClient().getChannelServer().getDropRate();
                        if (GM == false) {
                            name.append("#k" + (num + 1) + ": #v" + itemId + "# " + namez + ((chr.isGM()) ? "#d  爆物几率：" + (Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "%\r\n" : "\r\n") + "#b(掉落条件:" + (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("需要接受任务#r " + MapleQuest.getInstance(de.questid).getName() + " #b)\r\n") : "#r无#b)") + "\r\n");
                        } else {
                            name.append("#L" + itemId + "##k" + (num + 1) + ": #v" + itemId + "# " + namez + ((chr.isGM()) ? "#d  爆物几率：" + (Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "%(点击更改)\r\n" : "\r\n") + "#b(掉落条件:" + (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("需要接受任务#r " + MapleQuest.getInstance(de.questid).getName() + " #b)\r\n") : "#r无#b)") + "\r\n");
                        }
                        //                       name.append("#k" + (num + 1) + ": #v" + itemId + "# " + namez + " #d" +"%\r\n#b(掉落條件:" + (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("需要接取任務#r " + MapleQuest.getInstance(de.questid).getName() + " #b)\r\n") : "#r無#b)") + ((chr.isGM())?"掉落機率：" + (Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0) +"\r\n":"\r\n"));

                        num++;
                    } else {
                        error.append(itemId + "\r\n");
                    }
                }
            }
            if (GM == true) {
                name.append("\r\n#L" + 10000 + "##k" + (num + 1) + ": #b我要额外新增掉落物品!");
            }
            if (error.length() > 0) {
                chr.dropMessage(1, "无效的物品ID:\r\n" + error.toString());
            }
            if (name.length() > 0) {
                return name.toString();
            }

        }
        return "该怪物无任何爆物数据。";
    }

    public void gainBeans(int s) {
        getPlayer().gainBeans(s);
        c.sendPacket(MaplePacketCreator.updateBeans(this.c.getPlayer()));
    }

    public void openBeans() {//打开豆豆机界面
        c.sendPacket(MaplePacketCreator.openBeans(getPlayer().getBeans(), 0));
        c.getPlayer().dropMessage(5, "按住左右鍵可以调整力道,建议调好角度连续打,不要按暂停若九宮格卡住没反应重新打开豆豆机");
    }

    public void setMonsterRiding(int itemid) {//裝備不能正常裝備的坐騎
        short src = getClient().getPlayer().haveItemPos(itemid);
        if (src == 100) {
            c.getPlayer().dropMessage(5, "你没有当前骑宠。");
        } else {
            MapleInventoryManipulator.equip(c, src, (short) -18);
            c.getPlayer().dropMessage(5, "装备骑宠成功。");
        }
    }

    public int getRandom(int... args_all) {
        int args = args_all[Randomizer.nextInt(args_all.length)];
        return args;
    }

    public void OwlAdv(int point, int itemid) {
        owlse(this.c, point, itemid);
    }

    public static void owlse(MapleClient c, int point, int itemid) {
        int itemSearch = itemid;

        List<HiredMerchant> hms = new ArrayList<HiredMerchant>();
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            if (!cserv.searchMerchant(itemSearch).isEmpty()) {
                hms.addAll(cserv.searchMerchant(itemSearch));
            }
        }
        if (hms.size() > 0) {
            if (c.getPlayer().haveItem(5230000, 1)) {
                MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5230000, 1, true, false);
            } else if (c.getPlayer().getCSPoints(point) >= 5) {
                c.getPlayer().modifyCSPoints(point, -5, true);
            } else {
                c.getPlayer().dropMessage(1, "点数不足，无法查询！");
                if (NPCScriptManager.getInstance().getCM(c) != null) {
                    NPCScriptManager.getInstance().dispose(c);
                    c.sendPacket(MaplePacketCreator.enableActions());
                }
            }

            if (NPCScriptManager.getInstance().getCM(c) != null) {
                NPCScriptManager.getInstance().dispose(c);
            }
            c.sendPacket(MaplePacketCreator.getOwlSearched(itemSearch, hms));
        } else {
            if (NPCScriptManager.getInstance().getCM(c) != null) {
                NPCScriptManager.getInstance().dispose(c);
                c.sendPacket(MaplePacketCreator.enableActions());
            }
            c.getPlayer().dropMessage(1, "找不到物品");
        }
    }

    public void checkMobs(MapleCharacter chr) {
        if (this.getMap().getAllMonstersThreadsafe().size() <= 0) {
            sendOk("#地图上没有怪物哦!!。");
            dispose();
        }
        String msg = "玩家 #b" + chr.getName() + "#k 此地图怪物爆物查询:\r\n#r(若有任何爆物问题,请截图发给QQ群管理)\r\n#d";
        Iterator monster = getMap().getAllUniqueMonsters().iterator();
        while (monster.hasNext()) {
            Object monsterid = monster.next();
//            msg += "#L" + monsterid + "##o" + monsterid + "#" + ((chr.isGM()) ? " 代碼:" + monsterid + "#l\r\n" : "(查看)#l\r\n");
            msg += "#L" + monsterid + "##o" + monsterid + "#" + " 代码:" + monsterid + " (查看)#l\r\n";
        }
        sendOk(msg);
    }

    public void getMobs(int itemid) {
        MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        final List<Integer> mobs = MapleMonsterInformationProvider.getInstance().getMobByItem(itemid);
        String text = "#d这些怪物会掉落您查询的物品#k: \r\n\r\n";

        for (int i = 0; i < mobs.size(); i++) {
            int quest = 0;
            if (mi.getDropQuest(mobs.get(i)) > 0) {
                quest = mi.getDropQuest(mobs.get(i));
            }
            int chance = mi.getDropChance(mobs.get(i)) * getClient().getChannelServer().getDropRate();

            text += "#r#o" + mobs.get(i) + "##k " /*+ (Integer.valueOf(chance >= 999999 ? 1000000 : chance).doubleValue() / 10000.0) + "%" */ + (quest > 0 && MapleQuest.getInstance(quest).getName().length() > 0 ? ("#b需要进行 " + MapleQuest.getInstance(quest).getName() + " 任务来取得#k") : "") + "\r\n";

        }
        sendNext(text);
    }

    public Gashapon getGashapon() {
        return GashaponFactory.getInstance().getGashaponByNpcId(this.getNpc());
    }

    public void getGachaponMega(String msg, Item item, int quantity) {
        World.Broadcast.broadcastGashponmega(MaplePacketCreator.getGachaponMega(c.getPlayer().getName(), " : x" + quantity + "恭喜玩家 " + c.getPlayer().getName() + " 在" + msg + "获得！", item, (byte) 1, c.getPlayer().getClient().getChannel()));
    }

    public void getItemMegaphone(String msg, Item item, int quantity) {
        World.Broadcast.broadcastGashponmega(MaplePacketCreator.itemMegaphone("[" + msg + "] "/*c.getPlayer().getName()*/, " : x " + quantity + "恭喜玩家 " + c.getPlayer().getName() + " 在" + msg + "抽中！", item, c.getPlayer().getClient().getChannel()));
    }

    public void EnterCS(int mod) {
        c.getPlayer().setCsMod(mod);
        InterServerHandler.EnterCashShop(c, c.getPlayer(), false);
    }

    /*public int[] getSavedFaces() {
        return getPlayer().getSavedFaces();
    }

    public int getSavedFace(int sel) {
        return getPlayer().getSavedFace(sel);
    }

    public void setSavedFace(int sel, int id) {
        getPlayer().setSavedFace(sel, id);
    }

    public int[] getSavedHairs() {
        return getPlayer().getSavedHairs();
    }

    public int getSavedHair(int sel) {
        return getPlayer().getSavedHair(sel);
    }

    public void setSavedHair(int sel, int id) {
        getPlayer().setSavedHair(sel, id);
    }*/
    public int 判断角色ID() {
        return c.getPlayer().getId();
    }

    public String 开服名称() {
        return c.getChannelServer().getServerName();
    }

    public String getServerName() {
        return c.getChannelServer().getServerName();
    }

    public void 个人存档() {
        c.getPlayer().saveToDB(false, false);
    }

    public void 全服存档() {
        try {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                    if (chr == null) {
                        continue;
                    }
                    chr.saveToDB(false, false);
                }
            }
        } catch (Exception e) {
        }
    }

    public void 删除角色(int id) {
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps1 = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            String sqlstr = " delete from characters where id =" + id + "";
            ps1.executeUpdate(sqlstr);
            c.getPlayer().dropMessage(1, "角色删除成功。");
        } catch (SQLException ex) {
        }
    }

    public String 显示物品(int a) {
        String data = "";
        data = "#v" + a + "# #b#z" + a + "##k";
        return data;
    }

    public void 判断人气() {
        c.getPlayer().getFame();
    }

    public void 回收地图() {
        if (判断地图(c.getPlayer().getMapId()) <= 0) {
            final int 地图 = c.getPlayer().getMapId();
            记录地图(地图);
            c.getPlayer().dropMessage(1, "回收成功，此地图将在 5 分钟后被回收。");
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000 * 60 * 5);
                        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                            for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                                if (chr == null) {
                                    continue;
                                }
                                if (chr.getMapId() == 地图) {
                                    chr.getClient().getSession().close();
                                }
                            }
                        }
                        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                            cserv.getMapFactory().destroyMap(地图, true);
                            cserv.getMapFactory().HealMap(地图);
                        }
                        删除地图(地图);
                    } catch (InterruptedException e) {
                    }
                }
            }.start();
        } else {
            c.getPlayer().dropMessage(1, "回收失败，此地图在回收队列中。");
        }
    }

    public void 回收地图(final int a) {
        if (判断地图(a) <= 0) {
            final int 地图 = a;
            记录地图(地图);
            c.getPlayer().dropMessage(1, "回收成功，此地图将在 1 小时后被重置。");
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000 * 60 * 1);
                        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                            cserv.getMapFactory().destroyMap(地图, true);
                            cserv.getMapFactory().HealMap(地图);
                        }
                        删除地图(地图);
                    } catch (InterruptedException e) {
                    }
                }
            }.start();
        } else {
            c.getPlayer().dropMessage(1, "回收失败，此地图在回收队列中。");
        }
    }

    public void 记录地图(int a) {
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO map (id) VALUES ( ?)");
            ps.setInt(1, a);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {

        }
    }

    public void 删除地图(int a) {
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps1 = con.prepareStatement("SELECT * FROM map where id =" + a + "");
            ResultSet rs = ps1.executeQuery();
            if (rs.next()) {
                String sqlstr = " Delete from map where id = '" + a + "'";
                ps1.executeUpdate(sqlstr);
            }
        } catch (SQLException ex) {

        }
    }

    public int 判断地图(int a) {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM map where id =" + a + "");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                data += 1;
            }
            ps.close();
        } catch (SQLException ex) {
        }
        return data;
    }

    public void 怪物清除() {
        double range = Double.POSITIVE_INFINITY;
        MapleMap map = c.getPlayer().getMap();
        MapleMonster mob;
        List<MapleMapObject> monsters = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER));
        for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
            mob = (MapleMonster) monstermo;
            map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
        }
    }

    public void 切换频道(int id) {
        c.getPlayer().changeChannel(id);
    }

    public void 动画(String String) {
        c.sendPacket(MaplePacketCreator.showEffect(String));//裂屏特效
    }

    public void 动画2(String String) {
        c.sendPacket(UIPacket.AranTutInstructionalBalloon(String));
    }

    public void 动画3(String data) {
        c.sendPacket(UIPacket.ShowWZEffect(data));
    }

    public int 查询在线人数() {
        int count = 0;
        for (ChannelServer chl : ChannelServer.getAllInstances()) {
            count += chl.getPlayerStorage().getAllCharacters().size();
        }
        return count;
    }

    public void 给能力点(final int amount) {//给AP
        c.getPlayer().gainAp((short) amount);
    }

    public void 收能力点(final int amount) {//给AP
        c.getPlayer().gainAp((short) -amount);
    }

    public void 给技能点(final int amount) {//给AP
        c.getPlayer().gainSP((short) amount);
    }

    public void 收技能点(final int amount) {//给AP
        c.getPlayer().gainSP((short) -amount);
    }

    public int 给全服发点券(int 数量, int 类型) {
        int count = 0;

        try {
            if (数量 <= 0 || 类型 <= 0) {
                return 0;
            }

            if (类型 == 1 || 类型 == 2) {
                for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                    for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                        mch.modifyCSPoints(类型, 数量);
                        String cash = null;
                        if (类型 == 1) {
                            cash = "点券";
                        } else if (类型 == 2) {
                            cash = "抵用券";
                        }
                        //mch.startMapEffect("管理员发放" + 数量 + cash + "给在线的所有玩家！快感谢管理员吧！", 5121009);
                        count++;
                    }
                }
            } else if (类型 == 3) {
                for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                    for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                        // mch.modifyCSPoints(类型, 数量);
                        mch.gainMeso(数量, true);
                        //mch.startMapEffect("管理员发放" + 数量 + "冒险币给在线的所有玩家！快感谢管理员吧！", 5121009);
                        count++;
                    }
                }
            } else if (类型 == 4) {
                for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                    for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                        mch.gainExp(数量, true, false, true);
                        //mch.startMapEffect("管理员发放" + 数量 + "经验给在线的所有玩家！快感谢管理员吧！", 5121009);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            c.getPlayer().dropMessage("给全服发点券出错：" + e.getMessage());
        }

        return count;
    }

    public int 给当前频道发点券(int 数量, int 类型) {
        int count = 0;
        int chlId = c.getPlayer().getMap().getChannel();

        try {
            if (数量 <= 0 || 类型 <= 0) {
                return 0;
            }
            if (类型 == 1 || 类型 == 2) {
                for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                    if (cserv1.getChannel() != chlId) {
                        continue;
                    }
                    for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                        mch.modifyCSPoints(类型, 数量);
                        String cash = null;
                        if (类型 == 1) {
                            cash = "点券";
                        } else if (类型 == 2) {
                            cash = "抵用券";
                        }
                        //mch.startMapEffect("管理员发放" + 数量 + cash + "给在线的所有玩家！快感谢管理员吧！", 5121009);
                        count++;
                    }
                }
            } else if (类型 == 3) {
                for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                    if (cserv1.getChannel() != chlId) {
                        continue;
                    }
                    for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                        // mch.modifyCSPoints(类型, 数量);
                        mch.gainMeso(数量, true);
                        //mch.startMapEffect("管理员发放" + 数量 + "冒险币给在线的所有玩家！快感谢管理员吧！", 5121009);
                        count++;
                    }
                }
            } else if (类型 == 4) {
                for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                    if (cserv1.getChannel() != chlId) {
                        continue;
                    }
                    for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                        mch.gainExp(数量, true, false, true);
                        //mch.startMapEffect("管理员发放" + 数量 + "经验给在线的所有玩家！快感谢管理员吧！", 5121009);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            c.getPlayer().dropMessage("给当前频道发点券出错：" + e.getMessage());
        }

        return count;
    }

    public int 给当前地图发点券(int 数量, int 类型) {
        int count = 0;
        int mapId = c.getPlayer().getMapId();
        try {
            if (数量 <= 0 || 类型 <= 0) {
                return 0;
            }

            if (类型 == 1 || 类型 == 2) {
                for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                    for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                        if (mch.getMapId() != mapId) {
                            continue;
                        }
                        mch.modifyCSPoints(类型, 数量);
                        String cash = null;
                        if (类型 == 1) {
                            cash = "点券";
                        } else if (类型 == 2) {
                            cash = "抵用券";
                        }
                        //mch.startMapEffect("管理员发放" + 数量 + cash + "给在线的所有玩家！快感谢管理员吧！", 5121009);
                        count++;
                    }
                }
            } else if (类型 == 3) {
                for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                    for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                        if (mch.getMapId() != mapId) {
                            continue;
                        }
                        mch.gainMeso(数量, true);
                        //mch.startMapEffect("管理员发放" + 数量 + "冒险币给在线的所有玩家！快感谢管理员吧！", 5121009);
                        count++;
                    }
                }
            } else if (类型 == 4) {
                for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
                    for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                        if (mch.getMapId() != mapId) {
                            continue;
                        }
                        mch.gainExp(数量, true, false, true);
                        //mch.startMapEffect("管理员发放" + 数量 + "经验给在线的所有玩家！快感谢管理员吧！", 5121009);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            c.getPlayer().dropMessage("给当前地图发点券出错：" + e.getMessage());
        }

        return count;
    }

    public int 传送当前地图所有人到指定地图(int destMapId, Boolean includeSelf) {
        int count = 0;
        int myMapId = c.getPlayer().getMapId();
        int myId = c.getPlayer().getId();

        try {
            final MapleMap tomap = getMapFactory().getMap(destMapId);
            final MapleMap frommap = getMapFactory().getMap(myMapId);
            List<MapleCharacter> list = frommap.getCharactersThreadsafe();
            if (tomap != null && frommap != null && list != null && frommap.getCharactersSize() > 0) {
                for (MapleMapObject mmo : list) {
                    MapleCharacter chr = (MapleCharacter) mmo;
                    if (chr.getId() == myId) { // 自己
                        if (includeSelf) {
                            chr.changeMap(tomap, tomap.getPortal(0));
                            count++;
                        }
                    } else {
                        chr.changeMap(tomap, tomap.getPortal(0));
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            c.getPlayer().dropMessage("传送当前地图所有人到指定地图出错：" + e.getMessage());
        }

        return count;
    }

    public int 杀死当前地图所有人(Boolean includeSelf) {
        int count = 0;
        int myMapId = c.getPlayer().getMapId();
        int myId = c.getPlayer().getId();

        try {
            final MapleMap frommap = getMapFactory().getMap(myMapId);
            List<MapleCharacter> list = frommap.getCharactersThreadsafe();
            if (frommap != null && list != null && frommap.getCharactersSize() > 0) {
                for (MapleMapObject mmo : list) {
                    if (mmo != null) {
                        MapleCharacter chr = (MapleCharacter) mmo;
                        if (chr.getId() == myId) { // 自己
                            if (includeSelf) {
                                chr.setHp(0);
                                chr.updateSingleStat(MapleStat.HP, 0);
                                count++;
                            }
                        } else {
                            chr.setHp(0);
                            chr.updateSingleStat(MapleStat.HP, 0);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            c.getPlayer().dropMessage("杀死当前地图所有人出错：" + e.getMessage());
        }

        return count;
    }

    public int 复活当前地图所有人(Boolean includeSelf) {
        int count = 0;
        int myMapId = c.getPlayer().getMapId();
        int myId = c.getPlayer().getId();

        try {
            final MapleMap frommap = getMapFactory().getMap(myMapId);
            List<MapleCharacter> list = frommap.getCharactersThreadsafe();
            if (frommap != null && list != null && frommap.getCharactersSize() > 0) {
                for (MapleMapObject mmo : list) {
                    if (mmo != null) {
                        MapleCharacter chr = (MapleCharacter) mmo;
                        if (chr.getId() == myId) { // 自己
                            if (includeSelf) {
                                chr.getStat().setHp(chr.getStat().getMaxHp());
                                chr.updateSingleStat(MapleStat.HP, chr.getStat().getMaxHp());
                                chr.getStat().setMp(chr.getStat().getMaxMp());
                                chr.updateSingleStat(MapleStat.MP, chr.getStat().getMaxMp());
                                chr.dispelDebuffs();
                                count++;
                            }
                        } else {
                            chr.getStat().setHp(chr.getStat().getMaxHp());
                            chr.updateSingleStat(MapleStat.HP, chr.getStat().getMaxHp());
                            chr.getStat().setMp(chr.getStat().getMaxMp());
                            chr.updateSingleStat(MapleStat.MP, chr.getStat().getMaxMp());
                            chr.dispelDebuffs();
                            count++;
                        }

                    }
                }
            }
        } catch (Exception e) {
            c.getPlayer().dropMessage("复活当前地图所有人出错：" + e.getMessage());
        }

        return count;
    }

    public int 获取指定地图玩家数量(int mapId) {//var count = cm.获取指定地图玩家数量(910000000);
        return getMapFactory().getMap(mapId).characterSize();
    }

    public int 判断指定地图玩家数量(int mapId) {//var count = cm.获取指定地图玩家数量(910000000);
        return getMapFactory().getMap(mapId).characterSize();
    }

    public int 判断当前地图怪物数量() {
        return c.getPlayer().getMap().getAllMonstersThreadsafe().size();
    }

    public int 判断指定地图怪物数量(int a) {
        return getMap(a).getAllMonstersThreadsafe().size();
    }

    public int 判断当前地图玩家数量() {
        return c.getPlayer().getMap().getCharactersSize();
    }

    public int 随机数(int a) {
        return (int) Math.ceil(Math.random() * a);
    }

    public boolean 百分率(int q) {
        int a = (int) Math.ceil(Math.random() * 100);
        if (a <= q) {
            return true;
        } else {
            return false;
        }
    }

    public boolean 时装(final int itemId) {
        return MapleItemInformationProvider.getInstance().isCash(itemId);
    }

    public void 说明文字(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
        lastMsg = 0;
    }

    public void 判断是否结婚() {
        c.getPlayer().getMarriageId();
    }

    public void 玩家名字() {
        c.getPlayer().getName();
    }

    public void 对话结束() {
        NPCScriptManager.getInstance().dispose(c);
    }

    public void 结束对话() {
        NPCScriptManager.getInstance().dispose(c);
    }

    public int 获取最高等级() {
        int level = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT  `level` FROM characters WHERE gm = 0 ORDER BY `level` DESC LIMIT 1");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    level = rs.getInt("level");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }

        return level;
    }

    public String 获取最强家族名称() {
        String name = "";
        String level = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT `name`, `GP` FROM guilds  ORDER BY `GP` DESC LIMIT 1");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    name = rs.getString("name");
                    level = rs.getString("GP");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }

        return String.format("%s", name);
    }

    public int 获取自己等级排名() {
        int DATA = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT rank FROM (SELECT @rownum := @rownum + 1 AS rank, `name`, `level`, `id` FROM characters, (SELECT @rownum := 0) r WHERE gm = 0 ORDER BY `level` DESC) AS T1 WHERE `id` = ?");
            ps.setInt(1, c.getPlayer().getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DATA = rs.getInt("rank");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }
        return DATA;
    }

    public int 获取自己金币排名() {
        int DATA = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT rank FROM (SELECT @rownum := @rownum + 1 AS rank, `name`, `meso`, `id` FROM characters, (SELECT @rownum := 0) r WHERE gm = 0 ORDER BY `meso` DESC) AS T1 WHERE `id` = ?");
            ps.setInt(1, c.getPlayer().getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DATA = rs.getInt("rank");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }
        return DATA;
    }

    public String 获取家族名称(int guildId) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT name as DATA FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }
        return data;
    }

    public String 获取家族族长备注(int guildId) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT rank1title as DATA FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }
        return data;
    }

    public String 获取家族副族长备注(int guildId) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT rank2title as DATA FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {

        }
        return data;
    }

    public String 获取家族一级成员备注(int guildId) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT rank3title as DATA FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取家族名称出错 - 数据库查询失败：" + Ex);
        }
        return data;
    }

    public String 获取家族二级成员备注(int guildId) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT rank4title as DATA FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {

        }
        return data;
    }

    public String 获取家族三级成员备注(int guildId) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT rank5title as DATA FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }
        return data;
    }

    public String 获取家族族长ID(int guildId) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT leader as DATA FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }
        return data;
    }

    public int 家族成员数(int a) {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters ");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("guildid") == a) {
                        data += 1;
                    }
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }
        return data;
    }

    public int 角色ID取账号ID(int id) {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT accountid as DATA FROM characters WHERE id = ?");
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getInt("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
        }
        return data;
    }

    public String 账号取绑定QQ(String id) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT qq as DATA FROM accounts WHERE name = ?");
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("账号ID取账号、出错");
        }
        return data;
    }

    public String 账号ID取绑定QQ(String id) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT qq as DATA FROM accounts WHERE id = ?");
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("账号ID取账号、出错");
        }
        return data;
    }

    public String 角色ID取名字(int id) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT name as DATA FROM characters WHERE id = ?");
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取角色ID取名字出错 - 数据库查询失败：" + Ex);
        }
        if (data == null) {
            data = "匿名人士";
        }
        return data;
    }

    public void 全服公告(String text) {//公告类型
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, text));
    }

    public void 个人黄色字体公告(String message) {
        c.sendPacket(UIPacket.getTopMsg(message));
    }

    public void 全服黄色字体(String message) {
        for (ChannelServer cserv1 : ChannelServer.getAllInstances()) {
            for (MapleCharacter mch : cserv1.getPlayerStorage().getAllCharacters()) {
                c.sendPacket(UIPacket.getTopMsg(message));
            }
        }
    }

    public void 加运气(int luk) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setLuk((short) (item.getLuk() + luk));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加智力(int Int) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setInt((short) (item.getInt() + Int));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加敏捷(int dex) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setDex((short) (item.getDex() + dex));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加力量(int str) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setStr((short) (item.getStr() + str));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加命中率(int Acc) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setAcc((short) (item.getAcc() + Acc));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加跳跃力(int Jump) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setJump((short) (item.getJump() + Jump));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加移动速度(int Speed) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setSpeed((short) (item.getSpeed() + Speed));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加闪避率(int Avoid) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setAvoid((short) (item.getAvoid() + Avoid));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加魔法攻击(int matk) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setMatk((short) (item.getMatk() + matk));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加魔法防御(int Mdef) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setMdef((short) (item.getMdef() + Mdef));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加物理攻击(int watk) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setWatk((short) (item.getWatk() + watk));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加物理防御(int Wdef) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setWdef((short) (item.getWdef() + Wdef));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加升级次数(int upgr) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setUpgradeSlots((byte) (item.getUpgradeSlots() + upgr));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加最大生命值(int hp) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setHp((short) (item.getHp() + hp));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加最大法力值(int mp) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setMp((short) (item.getMp() + mp));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 装备洗练() {

        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();

        if (item.getUpgradeSlots() > 0) {
            int getUpgradeSlots = (int) Math.ceil(Math.random() * (item.getUpgradeSlots() + item.getUpgradeSlots() * 0.5));
            item.setUpgradeSlots((byte) getUpgradeSlots);
        }
        if (item.getWatk() > 0) {
            int getWatk = (int) Math.ceil(Math.random() * (item.getWatk() + item.getWatk() * 0.5));
            item.setWatk((byte) getWatk);
        }
        if (item.getMatk() > 0) {
            int getMatk = (int) Math.ceil(Math.random() * (item.getMatk() + item.getMatk() * 0.5));
            item.setMatk((byte) getMatk);
        }
        if (item.getWdef() > 0) {
            int getWdef = (int) Math.ceil(Math.random() * (item.getWdef() + item.getWdef() * 0.5));
            item.setWdef((byte) getWdef);
        }
        if (item.getMdef() > 0) {
            int getMdef = (int) Math.ceil(Math.random() * (item.getMdef() + item.getMdef() * 0.5));
            item.setMdef((byte) getMdef);
        }
        if (item.getStr() > 0) {
            int getStr = (int) Math.ceil(Math.random() * (item.getStr() + item.getStr() * 0.5));
            item.setStr((byte) getStr);
        }
        if (item.getDex() > 0) {
            int getDex = (int) Math.ceil(Math.random() * (item.getDex() + item.getDex() * 0.5));
            item.setDex((byte) getDex);
        }
        if (item.getLuk() > 0) {
            int getLuk = (int) Math.ceil(Math.random() * (item.getLuk() + item.getLuk() * 0.5));
            item.setLuk((byte) getLuk);
        }
        if (item.getInt() > 0) {
            int getInt = (int) Math.ceil(Math.random() * (item.getInt() + item.getInt() * 0.5));
            item.setInt((byte) getInt);
        }
        if (item.getHp() > 0) {
            int getHp = (int) Math.ceil(Math.random() * (item.getHp() + item.getHp() * 0.5));
            item.setHp((byte) getHp);
        }
        if (item.getMp() > 0) {
            int getMp = (int) Math.ceil(Math.random() * (item.getMp() + item.getMp() * 0.5));
            item.setMp((byte) getMp);
        }
        if (item.getAcc() > 0) {
            int getAcc = (int) Math.ceil(Math.random() * (item.getAcc() + item.getAcc() * 0.5));
            item.setAcc((byte) getAcc);
        }
        if (item.getAvoid() > 0) {
            int getAvoid = (int) Math.ceil(Math.random() * (item.getAvoid() + item.getAvoid() * 0.5));
            item.setAvoid((byte) getAvoid);
        }
        if (item.getSpeed() > 0) {
            int getSpeed = (int) Math.ceil(Math.random() * (item.getSpeed() + item.getSpeed() * 0.5));
            item.setSpeed((byte) getSpeed);
        }
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public int 查询今日在线时间() {
        int data = 0;
        data = c.getPlayer().getTodayOnlineTime();
        return data;
    }

    public int 查询总在线时间() {
        int data = 0;
        data = c.getPlayer().getTotalOnlineTime();
        return data;
    }

    public int 今日全服总在线时间() {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("todayOnlineTime") > 0) {
                    data += rs.getInt("todayOnlineTime");
                }
            }
        } catch (SQLException ex) {
        }
        return data;
    }

    public int 今日家族总在线时间(int a) {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("guildid") == a) {
                    if (rs.getInt("todayOnlineTime") > 0) {
                        data += rs.getInt("todayOnlineTime");
                    }
                }
            }
        } catch (SQLException ex) {
        }
        return data;
    }

    public void 是否说明文字(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.sendPacket(MaplePacketCreator.getNPCTalk(npc, (byte) 1, text, "", (byte) 0));
        lastMsg = 1;
    }

    public void 加宝石(int a) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) 1).copy();
        item.setMpRR((short) (item.getMpRR() + a));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) 1, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 加锻造等级(int a, int b) {
        Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) a).copy();
        item.setHpRR((short) (item.getHpRR() + b));
        MapleInventoryManipulator.removeFromSlot(getC(), MapleInventoryType.EQUIP, (short) a, (short) 1, true);
        MapleInventoryManipulator.addFromDrop(getChar().getClient(), item, false);
    }

    public void 小纸条(String type1, String type2) {
        c.getPlayer().sendNote(type1, type2);

    }

    public String 职业(int a) {
        return getJobNameById(a);
    }

    public String 查询爆物(int a) {
        StringBuilder name = new StringBuilder();
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM drop_data where itemid =" + a + "")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    name.append("    #o").append(rs.getInt("dropperid")).append("#\r\n");
                }
                ps.close();
            }
        } catch (SQLException ex) {
        }
        return name.toString();
    }

    public void 增加角色最大生命值(short hp) {//设置当前生命值
        Map<MapleStat, Integer> statup = new EnumMap<>(MapleStat.class);
        c.getPlayer().getStat().setMaxHp((short) (c.getPlayer().getStat().getMaxHp() + hp));
        statup.put(MapleStat.MAXHP, (int) c.getPlayer().getStat().getMaxHp());
        c.sendPacket(MaplePacketCreator.updatePlayerStats(statup, c.getPlayer()));
    }

    public void 增加角色最大法力值(short MP) {//设置当前生命值
        Map<MapleStat, Integer> statup = new EnumMap<>(MapleStat.class);
        c.getPlayer().getStat().setMaxMp((short) (c.getPlayer().getStat().getMaxMp() + MP));
        statup.put(MapleStat.MAXMP, (int) c.getPlayer().getStat().getMaxMp());
        c.sendPacket(MaplePacketCreator.updatePlayerStats(statup, c.getPlayer()));
    }

    public int 判断点券() {
        return c.getPlayer().getCSPoints(1);
    }

    public int 判断抵用券() {
        return c.getPlayer().getCSPoints(2);
    }

    public int 判断兑换卡是否存在(String id) {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT code as DATA FROM nxcodez WHERE code = ?");
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data += 1;
                }
            }
        } catch (SQLException Ex) {
            System.err.println("判断兑换卡是否存在、出错");
        }

        return data;
    }

    public int 判断兑换卡类型(String code) throws SQLException {
        int item = -1;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT `leixing` FROM nxcodez WHERE code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                item = rs.getInt("leixing");
            }
            rs.close();
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("判断兑换卡是否存在、出错");
        }
        return item;
    }

    public int 判断兑换卡数额(String code) throws SQLException {
        int item = -1;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT `valid` FROM nxcodez WHERE code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                item = rs.getInt("valid");
            }
            rs.close();
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("判断兑换卡是否存在、出错");
        }
        return item;
    }

    public int 判断兑换卡礼包(String code) throws SQLException {
        int item = -1;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT `itme` FROM nxcodez WHERE code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                item = rs.getInt("itme");
            }
            rs.close();
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("判断兑换卡是否存在、出错");
        }
        return item;
    }

    //删除兑换卡
    public void Deleteexchangecard(String a) {
        PreparedStatement ps1 = null;
        ResultSet rs = null;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            ps1 = con.prepareStatement("SELECT * FROM nxcodez ");
            rs = ps1.executeQuery();
            while (rs.next()) {
                String sqlstr = " delete from nxcodez where code = '" + a + "' ";
                ps1.executeUpdate(sqlstr);
            }
        } catch (SQLException ex) {
        }
    }

    public void 充值卡兑换记录(String msg1, String msg2) {
        FileoutputUtil.logToFile("充值卡兑换记录/" + CurrentReadable_Date() + "/" + msg1 + " 充值卡.txt", "" + msg2 + "\r\n");
    }

    public String 显示所有家族() {
        StringBuilder name = new StringBuilder();
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds order by GP desc");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int 家族编号 = rs.getInt("guildid");
                String 家族名字 = rs.getString("name");
                int 家族GP点 = 家族成员数(家族编号);
                name.append("  #L").append(家族编号).append("##d家族:#b").append(家族名字).append("#k");
                for (int j = 13 - 家族名字.getBytes().length; j > 0; j--) {
                    name.append(" ");
                }
                String 玩家名字 = 角色ID取名字(rs.getInt("leader"));
                name.append("#d族长:#r").append(玩家名字).append("#k");
                for (int j = 13 - 玩家名字.getBytes().length; j > 0; j--) {
                    name.append(" ");
                }
                name.append("#d人数:#r").append(家族GP点).append("#k#l\r\n");
            }
        } catch (SQLException ex) {
        }
        return name.toString();
    }

    public void 加入家族(int a, int b) {
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET `guildid` = ? WHERE id = ?");
            ps.setInt(1, a);
            ps.setInt(2, b);
            ps.execute();
            ps.close();

            c.getPlayer().setGuildId(a);
            c.getPlayer().setGuildRank((byte) 5);
            int s = World.Guild.addGuildMember(c.getPlayer().getMGC());
            if (s == 0) {
                c.getPlayer().dropMessage(1, "你想要加入的家族已经满员了，所以后续的操作可能已经无效了。");
                c.getPlayer().setGuildId(0);
                return;
            }
            c.sendPacket(MaplePacketCreator.showGuildInfo(c.getPlayer()));
            c.getPlayer().saveGuildStatus();
        } catch (SQLException sql) {
            System.err.println("加入家族错误:" + sql);
        }
    }

    public void 清怪() {
        MapleMap map = c.getPlayer().getMap();
        double range = Double.POSITIVE_INFINITY;
        MapleMonster mob;
        for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
            mob = (MapleMonster) monstermo;
            map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
        }
    }

    public String 显示商品(int id) {
        StringBuilder name = new StringBuilder();
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM mysterious WHERE f = " + id + "");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int 编号 = rs.getInt("a");
                int 物品 = rs.getInt("b");
                int 数量 = rs.getInt("c");
                int 点券 = rs.getInt("d");
                int 金币 = rs.getInt("e");
                name.append("   #L").append(编号).append("# #v").append(物品).append("# #b#t").append(物品).append("##k x ").append(数量).append("");
                name.append(" #d[券/币]:#b").append(点券).append("#k/#b").append(金币).append("#k#l\r\n");
            }
        } catch (SQLException ex) {
        }
        return name.toString();
    }

    public void 购买物品(int id) {
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM mysterious WHERE f = " + id + "");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int 编号 = rs.getInt("a");
                int 物品 = rs.getInt("b");
                int 数量 = rs.getInt("c");
                int 点券 = rs.getInt("d");
                int 金币 = rs.getInt("e");
                gainItem(物品, (short) 数量);
            }

        } catch (SQLException ex) {
        }
    }

    public int 神秘商人() {
        return 活动神秘商人.神秘商人时间;
    }

    public int 神秘商人2() {
        return 活动神秘商人.神秘商人频道;
    }

    public int 神秘商人3() {
        return 活动神秘商人.神秘商人地图;
    }

    public int 通缉令1() {
        return 活动野外通缉.通缉BOSS;
    }

    public int 通缉令2() {
        return 活动野外通缉.通缉地图;
    }

    public void 重置野外通缉() {
        活动野外通缉.随机通缉();
        Start.初始通缉令 = 0;
    }

    public void 每日送货奖励() {
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement(" SELECT * FROM  每日送货奖励 ");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double 概率 = Math.ceil(Math.random() * rs.getInt("chance"));
                    double 对比 = Math.ceil(Math.random() * 100);
                    double 最少 = Math.ceil(Math.random() * rs.getInt("baseQty"));
                    double 最多 = Math.ceil(Math.random() * rs.getInt("maxRandomQty"));
                    if (最多 <= 最少) {
                        最多 = 最少;
                    }
                    if (概率 >= 100) {
                        概率 = 100;
                    }
                    if (概率 <= 对比) {
                        gainItem(rs.getInt("itemId"), (short) 最多);
                    }
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("每日送货奖励、出错");
        }
    }

    public void 每日送货奖励2() {
        // itemId baseQty maxRandomQty chance
        int count = 0;
        int arrLength = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT COUNT(itemId) as Count, SUM(chance) as Data FROM 每日送货奖励");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("Count");
                    arrLength = rs.getInt("Data");
                }
            }
            ps.close();
        } catch (SQLException ex) {
            System.err.println("查询每日送货奖励之概率分母出错：" + ex.getMessage());
        }

        if (count == 0 || arrLength == 0) {
            return;
        }

        int[][] data = new int[arrLength][2];

        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT itemId, baseQty, maxRandomQty, chance FROM 每日送货奖励");
            try (ResultSet rs = ps.executeQuery()) {
                int j = 0;
                while (rs.next()) {
                    int randomQty = new Random().nextInt(rs.getInt("maxRandomQty"));
                    for (int i = 0; i <= rs.getInt("chance") - 1; i++) {
                        data[j] = new int[]{rs.getInt("itemId"), rs.getInt("baseQty") + randomQty};
                        j++;
                    }
                }
            }
            ps.close();
        } catch (SQLException ex) {
            System.err.println("查询每日送货奖励出错：" + ex.getMessage());
        }

        List<int[]> result = new ArrayList<>();
        for (int i = 0; i <= count - 1; i++) {
            int r = new Random().nextInt(arrLength - 1);

            int itemId = data[r][0];
            int existsCount = 0;
            for (int[] is : result) {
                if (is[0] == itemId) {
                    existsCount++;
                }
            }
            if (existsCount > 0) {
                continue;
            }

            result.add(data[r]);
        }

        if (result.size() > 0) {
            for (int[] is : result) {
                int itemId = is[0];
                short qty = (short) is[1];
                gainItem(itemId, qty);
            }
        }
    }

    /**
     * <9>
     */
    public int 判断第一阶段是否完成() {
        int a = 0;
        if (c.getPlayer().getBossLogD("送货104000100") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货104000200") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货104000300") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货104000400") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货104010000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货104020000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货104030000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货104040000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货100000000") > 0) {
            a += 1;
        }
        return a;
    }

    /**
     * <7>
     */
    public int 判断第二阶段是否完成() {
        int a = 0;
        if (c.getPlayer().getBossLogD("送货100010000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货100020000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货100030000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货100040000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货100040100") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货100050000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货101000000") > 0) {
            a += 1;
        }
        return a;
    }

    /**
     * <10>
     */
    public int 判断第三阶段是否完成() {
        int a = 0;
        if (c.getPlayer().getBossLogD("送货101010000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货101010100") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货101020000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货101030000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货101030100") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货101030200") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货101030300") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货101030400") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货101040000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货102000000") > 0) {
            a += 1;
        }
        return a;
    }

    /**
     * <6>
     */
    public int 判断第四阶段是否完成() {
        int a = 0;
        if (c.getPlayer().getBossLogD("送货102010000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货102020000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货102030000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货102040000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货102050000") > 0) {
            a += 1;
        }
        if (c.getPlayer().getBossLogD("送货103000000") > 0) {
            a += 1;
        }

        return a;
    }

    public void 给豆豆(int s) {//给豆豆
        getPlayer().gainBeans(s);
        c.sendPacket(MaplePacketCreator.updateBeans(c.getPlayer()));
    }

    public void 收豆豆(int s) {//给豆豆
        getPlayer().gainBeans(-s);
        c.sendPacket(MaplePacketCreator.updateBeans(c.getPlayer()));
    }

    public void 刷新() {//刷新
        MapleCharacter player = c.getPlayer();
        c.sendPacket(MaplePacketCreator.getCharInfo(player));
        player.getMap().removePlayer(player);
        player.getMap().addPlayer(player);
    }

    public int 取个人副本通关时间最快(int a, int b) {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM fubenjilu WHERE name = " + a + " && cid = " + b + "order by time asc");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getInt("time");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("取副本通关时间最快 - 数据库查询失败：" + Ex);
        }
        return data;
    }

    public int 取副本通关最快时间(int a) {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM fubenjilu WHERE name = " + a + " order by time asc");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getInt("time");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("取副本通关时间最快 - 数据库查询失败：" + Ex);
        }
        return data;
    }

    public int 取副本通关最快玩家(int a) {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM fubenjilu WHERE name = " + a + " order by time asc");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getInt("cid");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("取副本通关时间最快 - 数据库查询失败：" + Ex);
        }
        return data;
    }

    public int 取副本通关时间(int a, int b) {
        int data = 0;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT time as DATA FROM fubenjilu WHERE name = " + a + " && cid = " + b + "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getInt("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("取副本通关时间 - 数据库查询失败：" + Ex);
        }
        return data;
    }

    public void 打开网页(String web) {//打开网址
        this.c.sendPacket(MaplePacketCreator.openWeb(web));
    }

    public String checkMapDrop() {
        List ranks = new ArrayList(MapleMonsterInformationProvider.getInstance().getGlobalDrop());
        int mapid = this.c.getPlayer().getMap().getId();
        //int cashServerRate = getClient().getChannelServer().getCashRate();
        int globalServerRate = 1;
        if ((ranks != null) && (ranks.size() > 0)) {
            int num = 0;

            StringBuilder name = new StringBuilder();
            for (int i = 0; i < ranks.size(); i++) {
                MonsterGlobalDropEntry de = (MonsterGlobalDropEntry) ranks.get(i);
                //if ((de.continent < 0) || ((de.continent < 10) && (mapid / 100000000 == de.continent)) || ((de.continent < 100) && (mapid / 10000000 == de.continent)) || ((de.continent < 1000) && (mapid / 1000000 == de.continent))) {

                int itemId = de.itemId;
                if (num == 0) {
                    name.append("#r" + LoginServer.getServerName() + "#k全局爆物预览；\r\n\r\n");
                    /*name.append("当前地图 #r").append(mapid).append("#k - #m").append(mapid).append("# 的全局爆率为:");
                        name.append("\r\n--------------------------------------\r\n");*/
                }

                String names = new StringBuilder().append("#z").append(itemId).append("#").toString();
                //if ((itemId == 0) && (cashServerRate != 0)) {
                //    itemId = 4031041;
                //    names = new StringBuilder().append(de.Minimum * cashServerRate).append(" - ").append(de.Maximum * cashServerRate).append(" 的抵用卷").toString();
                //}
                int chance = de.chance * globalServerRate;
                if (getPlayer().isAdmin()) {
                    name.append(num + 1).append(".  #b#v").append(itemId).append("#").append(names).append(getPlayer().isGM() ? "#k 爆率: " : "").append(getPlayer().isGM() ? Integer.valueOf(chance >= 999999 ? 1000000 : chance).doubleValue() / 10000.0D : "").append(getPlayer().isGM() ? " %" : "").append((de.questid > 0) && (MapleQuest.getInstance(de.questid).getName().length() > 0) ? new StringBuilder().append("需要接受任务: ").append(MapleQuest.getInstance(de.questid).getName()).toString() : "").append("\r\n");
                } else {
                    name.append(num + 1).append(".  #b#v").append(itemId).append("#").append(names).append((de.questid > 0) && (MapleQuest.getInstance(de.questid).getName().length() > 0) ? new StringBuilder().append("需要接受任务: ").append(MapleQuest.getInstance(de.questid).getName()).toString() : "").append("\r\n");
                }
                num++;
                //}
            }
            if (name.length() > 0) {
                return name.toString();
            }
        }
        return "全局爆物数据为空。";

    }

    public void completeQuest() {
        forceCompleteQuest();
    }

    public String 角色名字取等级(String id) {
        String data = "";
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT level as DATA FROM characters WHERE name = ?");
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data = rs.getString("DATA");
                }
            }
            ps.close();
        } catch (SQLException Ex) {
            System.err.println("获取角色名字取ID出错 - 数据库查询失败：" + Ex);
        }
        if (data == null) {
            data = "匿名人士";
        }
        return data;
    }

    public void 写记录(int A, int B, int C) {
        PreparedStatement ps1 = null;
        ResultSet rs = null;
        try (Connection con = DBConPool.getInstance().getDataSource().getConnection()) {
            ps1 = con.prepareStatement("SELECT * FROM fubenjilu ");
            rs = ps1.executeQuery();
            if (rs.next()) {
                String sqlstr = " Delete from fubenjilu where name ='" + A + "' && cid = " + B + "";
                ps1.executeUpdate(sqlstr);
            }
            PreparedStatement ps = con.prepareStatement("INSERT INTO fubenjilu (name, cid,time) VALUES (?,?,?)");
            ps.setInt(1, A);
            ps.setInt(2, B);
            ps.setInt(3, C);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex);
        }
    }

}
