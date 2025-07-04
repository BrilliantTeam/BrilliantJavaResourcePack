![關於輝煌](https://www.brilliantw.net/img/關於輝煌/橫幅.png)

玩家可於 [Assets](https://github.com/BrilliantTeam/BrilliantJavaResourcePack/releases/latest) 條目中，

點選 bsje-n.zip 來下載此版本的材質包

建議玩家下載 Version 標題旁有綠色 Latest 字樣版本的材質包來登入輝煌伺服器，

以取得最完善的遊戲體驗。

Copyright © 輝煌伺服器 Brilliant Minecraft Server 版權所有 All rights reserved

## 資源包跨版運作細節
由` pack.mcmeta `區分目錄，利用` overlays `實現版本控制  
- ` pack `
  - ` pack_format `：資源包的主要支援版本
  - ` description `：資源包描述
- ` supported_formats `：指定支援的` pack_format `版本   
  - ` from `：從某` pack_format `版本（低）   
  - ` to `：到某` pack_format `版本（高）   
- ` overlays `   
  - ` entries `   
    - ` directory `：指定根資料夾（` <名稱>/assets `）   
    - ` formats `：指定` pack_format `版本（低, 高）   

## 當前已有版本
- ` 1.20 `至` 1.21.3 `（` pack_format `：` 15 `）   
  - 路徑：` assets `
  - ` formats `：` 15 `（使用` 1.20 ~ 1.21.6 `客戶端時，將載入到該根資料夾之資料，為主要材質模型設置資料夾）   
- ` 1.21.4 `至` 1.21.5 `（` pack_format `：` 46 `）   
  - 改動：將` models/item `更改於` items `實現，並更改 JSON 寫法   
  - 路徑：` 1.21.4/assets `（僅包含需改動內容）   
  - ` formats `：` 46, 63 `（使用` 1.21.4 ~ 1.21.6 `客戶端時，將載入到該根資料夾之資料，並覆蓋相同的路徑文件【如果有】）
    - 目前會覆蓋：無，僅會多載入一個` items `資料夾
- ` 1.21.6 `以上（` pack_format `：` 63 `）
  - 改動：將` player_head `的 JSON 寫法適配於` 1.21.6 `以上版本   
  - 路徑：` 1.21.6/assets `（僅包含需改動內容）   
  - ` formats `：` 63, 63 `（使用` 1.21.6 `客戶端時，將載入到該根資料夾之改動資料，並覆蓋相同的路徑文件【如果有】）
    - 目前會覆蓋：` player_head.json `（` 1.21.6 `蓋掉` 1.21.4 `）

## 結構描述
- ` .github/workflows `：Github Actions 設定
  - ` main.yml `：主要設定檔
- ` 1.21.4/... `：跨版運作根目錄之一
- ` 1.21.6/... `：跨版運作根目錄之二
- ` assets `：主要材質設定目錄
  - ` blockentities `：自定義方塊插件材質模型使用
    - ` models/item/... `：自定義方塊插件模型 json
    - ` textures/block/... `：自定義方塊模型材質 png
  - ` minecraft `：原版材質模型使用
    - ` font/default.json `：自定義字型 json
    - ` lang/... `：自定義語言翻譯 json
    - ` models `：模型相關目錄
      - ` custom_items/... `：自定義方塊插件模型 json（主要模型設定）
      - ` item/... `：自定義方塊插件模型 json（設定` custom model data `）
        - ` 1.21.4 `後已移動致上一層級目錄，為` items `資料夾
    - ` sounds/... `：自定義音效檔案 ogg
    - ` textures `：材質相關目錄
      - ` block/... `：更改水的透明度材質 png（流動）
      - ` emojis/... `：自定義字型材質 png（表符）
      - ` gui/title/... `：使用模組才能看到的載入圖示 png
      - ` item/... `：自定義方塊模型材質 png
      - ` logo/... `：自定義字型材質 png（少數情況使用，現已大部分整合入` textures/emojis ` ）
      - ` misc/... `：更改水的透明度材質 png（靜止）
    - ` sounds.json `：自定義音效設定 json
- ` file.toml `：加密相關設定
- ` LICENSE.txt `：開源協議
- ` pack.mcmeta `：資源包設定
- ` pack.png `：資源包圖示
- ` README.md `：本文件，自述說明