# netapi

allmusic 音乐api示范

## 播放VIP歌曲
1. 手机号创建网易云账户并购买网易云音乐VIP
2. 在给自己服务器管理员权限
   - paper/folia 服务器给自己op
   - forge/fabric/neoforge 服务器给自己等级权限2
   - velocity 需要配置文件写上自己的游戏名
3. 使用支持安装插件的浏览器，打开music.163.com登录账户
4. 给浏览器安装插件 https://cookie-editor.com/
5. 打开插件，然后复制cookie，选择json
![](./img/pic1.png)
6. 打开插件配置文件，覆盖`cookie.json`，输入/music reload
