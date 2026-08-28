package netapi;

import com.coloryr.allmusic.server.core.AllMusic;
import com.coloryr.allmusic.server.core.IMusicApi;
import com.coloryr.allmusic.server.core.music.LyricSave;
import com.coloryr.allmusic.server.core.objs.HttpResObj;
import com.coloryr.allmusic.server.core.objs.SearchMusicObj;
import com.coloryr.allmusic.server.core.objs.message.ARG;
import com.coloryr.allmusic.server.core.objs.music.SearchPageObj;
import com.coloryr.allmusic.server.core.objs.music.SongInfoObj;
import com.coloryr.allmusic.server.core.saves.MusicListSave;
import com.coloryr.allmusic.server.core.utils.Function;
import netapi.obj.ConfigObj;
import netapi.obj.music.info.InfoObj;
import netapi.obj.music.list.DataObj;
import netapi.obj.music.lyric.WLyricObj;
import netapi.obj.music.search.SearchDataObj;
import netapi.obj.music.search.songs;
import netapi.obj.music.trialinfo.TrialInfoObj;
import netapi.obj.program.info.PrInfoObj;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class NetiApiMain implements IMusicApi {

    private boolean isUpdate;

    private File configFile;
    private ConfigObj config;

    public NetiApiMain() {
        AllMusic.log.data("<light_purple>[AllMusic3]<yellow>正在初始化网络爬虫");

        HttpResObj res = NetApiHttpClient.get("https://music.163.com", "");
        if (res == null || !res.ok) {
            AllMusic.log.data("<light_purple>[AllMusic3]<red>初始化net api失败");
        }
    }

    @Override
    public boolean isBusy() {
        return isUpdate;
    }

    @Override
    public String getMusicId(String arg) {
        if (arg.contains("id=") && !arg.contains("/?userid")) {
            if (arg.contains("&uct2")) {
                return Function.getString(arg, "id=", "&uct2");
            } else if (arg.contains("&user"))
                return Function.getString(arg, "id=", "&user");
            else
                return Function.getString(arg, "id=", null);
        } else if (arg.contains("song/")) {
            if (arg.contains("/?userid"))
                return Function.getString(arg, "song/", "/?userid");
            else
                return Function.getString(arg, "song/", null);
        } else
            return arg;
    }

    @Override
    public boolean checkId(String id) {
        return Function.isInteger(id);
    }

    @Override
    public void command(Object sender, String s, String[] strings) {

    }

    @Override
    public List<String> tab(Object sender, String s, String[] strings) {
        return new ArrayList();
    }

    /**
     * 获取音乐详情
     *
     * @param id     音乐ID
     * @param player 用户名
     * @param isList 是否是空闲列表
     * @return 结果
     */
    private SongInfoObj getMusicDetail(String id, String player, boolean isList) {
        JsonObject params = new JsonObject();
        params.addProperty("c", "[{\"id\":" + id + "}]");

        HttpResObj res = NetApiHttpClient.post("https://music.163.com/api/v3/song/detail", params, EncryptType.WEAPI, null);
        if (res != null && res.ok) {
            InfoObj temp = AllMusic.gson.fromJson(res.data, InfoObj.class);
            if (temp.isOk()) {
                params = new JsonObject();
                params.addProperty("ids", "[" + id + "]");
                params.addProperty("br", "320000");
                res = NetApiHttpClient.post("https://music.163.com/weapi/song/enhance/player/url", params, EncryptType.WEAPI, null);
                if (res == null || !res.ok) {
                    AllMusic.log.data("<light_purple>[AllMusic3]<red>版权检索失败");
                    return null;
                }
                TrialInfoObj obj = AllMusic.gson.fromJson(res.data, TrialInfoObj.class);
                com.coloryr.allmusic.server.core.objs.music.TrialInfoObj info = new com.coloryr.allmusic.server.core.objs.music.TrialInfoObj();
                info.start = obj.getFreeTrialInfo().getStart();
                info.end = obj.getFreeTrialInfo().getEnd();
                return new SongInfoObj(temp.getAuthor(), temp.getName(),
                        id, temp.getAlia(), player, temp.getAl(), isList, temp.getLength(),
                        temp.getPicUrl(), obj.isTrial(), info,
                        getId());
            }
        }
        return null;
    }

    @Override
    public void reload(File file) {
        configFile = new File(file, "netapi.json");

        if (configFile.exists()) {
            try {
                InputStreamReader reader = new InputStreamReader(
                        Files.newInputStream(configFile.toPath()), StandardCharsets.UTF_8);
                BufferedReader bf = new BufferedReader(reader);
                config = AllMusic.gson.fromJson(bf, ConfigObj.class);
                bf.close();
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            config = ConfigObj.make();
            try {
                String data = AllMusic.gson.toJson(config);
                FileOutputStream out = new FileOutputStream(configFile);
                OutputStreamWriter write = new OutputStreamWriter(
                        out, StandardCharsets.UTF_8);
                write.write(data);
                write.close();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getId() {
        return "netapi";
    }

    /**
     * 获取音乐数据
     *
     * @param id     音乐ID
     * @param player 用户名
     * @param isList 是否是空闲列表
     * @return 结果
     */
    public SongInfoObj getMusic(String id, String player, boolean isList) {
        SongInfoObj info = getMusicDetail(id, player, isList);
        if (info != null)
            return info;
        JsonObject params = new JsonObject();
        params.addProperty("id", id);
        HttpResObj res = NetApiHttpClient.post("https://music.163.com/api/dj/program/detail", params, EncryptType.WEAPI, null);
        if (res != null && res.ok) {
            PrInfoObj temp = AllMusic.gson.fromJson(res.data, PrInfoObj.class);
            if (temp.isOk()) {
                return new SongInfoObj(temp.getAuthor(), temp.getName(),
                        temp.getId(), temp.getAlia(), player, "电台", isList, temp.getLength(),
                        null, false, null, getId());
            } else {
                AllMusic.log.data("<light_purple>[AllMusic3]<red>歌曲信息获取为空");
            }
        }
        return info;
    }

    /**
     * 获取播放链接
     *
     * @param id 音乐ID
     * @return 结果
     */
    public String getPlayUrl(String id) {
        JsonObject params = new JsonObject();
        params.addProperty("ids", "[" + id + "]");
        if (config.level != null) {
            params.addProperty("level", config.level);
        } else {
            params.addProperty("level", "exhigh");
        }
        if (config.encodeType != null) {
            params.addProperty("encodeType", config.encodeType);
        } else {
            params.addProperty("encodeType", "aac");
        }
        HttpResObj res = NetApiHttpClient.post("https://music.163.com/weapi/song/enhance/player/url/v1", params, EncryptType.WEAPI, null);
        if (res != null && res.ok) {
            try {
                TrialInfoObj obj = AllMusic.gson.fromJson(res.data, TrialInfoObj.class);
                return obj.getUrl();
            } catch (Exception e) {
                AllMusic.log.data("<light_purple>[AllMusic3]<red>播放连接解析错误：" + res.data);
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 添加空闲歌单
     *
     * @param id     歌单id
     * @param sender 发送者
     */
    public void setList(String id, Object sender) {
        final Thread thread = new Thread(() -> {
            JsonObject params = new JsonObject();
            params.addProperty("id", id);
            params.addProperty("n", 100000);
            params.addProperty("s", 8);
            HttpResObj res = NetApiHttpClient.post("https://music.163.com/api/v6/playlist/detail", params, EncryptType.API, null);
            if (res != null && res.ok)
                try {
                    isUpdate = true;
                    DataObj obj = AllMusic.gson.fromJson(res.data, DataObj.class);
                    MusicListSave.addIdleList(obj.getPlaylist(), getId());
                    AllMusic.side.sendMessageTask(sender, AllMusic.getMessage().musicPlay.listMusic.get.replace(ARG.name, obj.getName()));
                } catch (Exception e) {
                    AllMusic.log.data("<light_purple>[AllMusic3]<red>歌曲列表获取错误");
                    e.printStackTrace();
                }
            isUpdate = false;
        }, "AllMusic_setList");
        thread.start();
    }

    /**
     * 获取歌词
     *
     * @param id 歌曲id
     * @return 结果
     */
    public LyricSave getLyric(String id) {
        LyricSave lyric = new LyricSave();
        JsonObject params = new JsonObject();
        params.addProperty("id", id);
        params.addProperty("cp", false);
        params.addProperty("tv", 0);
        params.addProperty("lv", 0);
        params.addProperty("rv", 0);
        params.addProperty("kv", 0);
        params.addProperty("yv", 0);
        params.addProperty("ytv", 0);
        params.addProperty("rtv", 0);
        HttpResObj res = NetApiHttpClient.post("https://interface3.music.163.com/eapi/song/lyric/v1",
                params, EncryptType.EAPI, "/api/song/lyric/v1");
        if (res != null && res.ok) {
            try {
                WLyricObj obj = AllMusic.gson.fromJson(res.data, WLyricObj.class);
                LyricDecoder docoder = new LyricDecoder();
                for (int times = 0; times < 3; times++) {
                    if (docoder.check(obj)) {
                        AllMusic.log.data("<light_purple>[AllMusic3]<red>歌词解析错误，正在进行第" + times + "重试");
                    } else {
                        if (docoder.isHave) {
                            lyric.setHaveLyric(AllMusic.getConfig().sendLyric);
                            lyric.setLyric(docoder.getLyrics());
                            if (docoder.isHaveK) {
                                lyric.setKlyric(docoder.getKLyric());
                            }
                        }
                        return lyric;
                    }
                    Thread.sleep(1000);
                }
                AllMusic.log.data("<light_purple>[AllMusic3]<red>歌词解析失败");
            } catch (Exception e) {
                AllMusic.log.data("<light_purple>[AllMusic3]<red>歌词解析错误");
                e.printStackTrace();
            }
        }
        return lyric;
    }

    /**
     * 搜歌
     *
     * @param name 关键字
     * @return 结果
     */
    public SearchPageObj search(String[] name) {
        List<SearchMusicObj> resData = new ArrayList<>();
        int maxpage;

        StringBuilder name1 = new StringBuilder();
        for (int a = 0; a < name.length; a++) {
            name1.append(name[a]).append(" ");
        }
        String MusicName = name1.toString();
        MusicName = MusicName.substring(0, MusicName.length() - 1);

        JsonObject params = new JsonObject();
        params.addProperty("s", MusicName);
        params.addProperty("type", 1);
        params.addProperty("limit", 30);
        params.addProperty("offset", 0);

        HttpResObj res = NetApiHttpClient.post("https://music.163.com/weapi/search/get", params, EncryptType.WEAPI, null);
        if (res != null && res.ok) {
            SearchDataObj obj = AllMusic.gson.fromJson(res.data, SearchDataObj.class);
            if (obj != null && obj.isOk()) {
                List<songs> res1 = obj.getResult();
                SearchMusicObj item;
                for (songs temp : res1) {
                    item = new SearchMusicObj(String.valueOf(temp.getId()), temp.getName(),
                            temp.getArtists(), temp.getAlbum());
                    resData.add(item);
                }
                maxpage = res1.size() / 10;
                return new SearchPageObj(resData, maxpage, getId());
            } else {
                AllMusic.log.data("<light_purple>[AllMusic3]<red>歌曲搜索出现错误");

            }
        }
        return null;
    }
}
