# AskMod — Fabric 1.21.1

Mod cho phép người chơi gõ `/ask <câu hỏi>` để hỏi một AI thật và nhận câu trả lời ngay trong chat.

Hỗ trợ 4 nhà cung cấp (provider), chọn 1 cái để dùng tại một thời điểm:

| provider | Model AI | Lấy API key tại |
|---|---|---|
| `anthropic` | Claude | https://console.anthropic.com |
| `openai` | ChatGPT | https://platform.openai.com |
| `gemini` | Google Gemini | https://aistudio.google.com/apikey |
| `openrouter` | Nhiều model qua 1 key (Claude, GPT, Gemini, Llama,...) | https://openrouter.ai/keys |

> **OpenRouter là gì?** Đây là một API trung gian: bạn chỉ cần **1 key OpenRouter** rồi chọn model bất kỳ bằng cách đổi tên model, ví dụ `anthropic/claude-3.5-haiku`, `openai/gpt-4o-mini`, `google/gemini-2.0-flash-exp`,... mà không cần key riêng của từng hãng.

## 1. Cấu trúc project

```
askmod-fabric/
├── build.gradle
├── settings.gradle
├── gradle.properties
└── src/main/
    ├── java/com/example/askmod/
    │   ├── AskMod.java        # entrypoint của mod
    │   ├── AskCommand.java    # đăng ký lệnh /ask và /ask admin
    │   ├── AskConfig.java     # đọc/ghi config/askmod.json, quản lý nhiều provider
    │   └── AIClient.java      # gọi API Anthropic / OpenAI / Gemini / OpenRouter
    └── resources/
        └── fabric.mod.json
```

## 2. Build mod

Cần **JDK 21**. Trong thư mục `askmod-fabric/`:

```bash
# Linux/macOS
./gradlew build

# Windows
gradlew.bat build
```

File `.jar` xuất ra ở `build/libs/askmod-1.0.0.jar`. Copy file này vào thư mục `mods/` của server hoặc client Fabric 1.21.1 (nhớ cài sẵn **Fabric API** và **Fabric Loader** cùng phiên bản).

> Lưu ý: mình không build thử được project này (môi trường soạn code không có mạng để tải Minecraft/Gradle). Nếu Gradle báo lỗi "không tìm thấy version" cho `yarn_mappings`, `loader_version` hoặc `fabric_version`, hãy vào https://fabricmc.net/develop để lấy đúng số phiên bản mới nhất cho **1.21.1** rồi sửa lại trong `gradle.properties`.

## 3. Cấu hình API key

Chạy server (hoặc client) lần đầu để mod tự tạo file `config/askmod.json`:

```json
{
  "provider": "anthropic",
  "anthropicApiKey": "",
  "anthropicModel": "claude-3-5-haiku-20241022",
  "openaiApiKey": "",
  "openaiModel": "gpt-4o-mini",
  "geminiApiKey": "",
  "geminiModel": "gemini-2.0-flash",
  "openrouterApiKey": "",
  "openrouterModel": "anthropic/claude-3.5-haiku",
  "systemPrompt": "Bạn là một trợ lý AI thân thiện trong game Minecraft...",
  "maxTokens": 300,
  "cooldownSeconds": 8,
  "adminPermissionLevel": 2
}
```

Có 2 cách điền key:

**Cách A — sửa file trực tiếp** (khuyên dùng cho lần đầu, vì lệnh in-game sẽ lưu key vào lịch sử chat/log server):
1. Dán API key vào đúng dòng provider muốn dùng (`anthropicApiKey`, `openaiApiKey`, `geminiApiKey` hoặc `openrouterApiKey`).
2. Đổi `"provider"` thành tên tương ứng.
3. Lưu file, chạy `/ask admin reload` (hoặc khởi động lại server).

**Cách B — dùng lệnh admin trong game** (xem mục 5 bên dưới), tiện khi cần đổi nhanh mà không vào được máy chủ file.

Nếu bạn đang nâng cấp từ bản mod cũ (chỉ có `apiKey` + `model` ở cấp gốc), mod sẽ **tự động chuyển** 2 giá trị đó vào đúng provider đang chọn khi khởi động lần đầu sau khi cập nhật — không cần làm gì thêm.

## 4. Sử dụng trong game

```
/ask Làm thế nào để chế tạo kim cương giáp?
/ask Con Ender Dragon mạnh cỡ nào?
```

Mod sẽ hiện "Đang suy nghĩ..." rồi trả lời trong chat với tiền tố `[AI]`. Có cooldown giữa 2 lần hỏi của cùng 1 người chơi để tránh spam/tốn phí API (`cooldownSeconds`, mặc định 8 giây).

## 5. Lệnh admin — `/ask admin ...` (yêu cầu quyền, chỉ server)

Các lệnh dưới đây chỉ chạy được nếu người chơi/console có **permission level** đủ lớn — mặc định là **level 2**, tương đương quyền OP cơ bản trong Minecraft vanilla (`/op <tên>` cấp level 4, đủ dùng mọi lệnh admin). Người chơi thường (không phải OP) sẽ không thấy/gõ được các lệnh này — kể cả gõ tay, server sẽ từ chối vì thiếu quyền.

| Lệnh | Chức năng |
|---|---|
| `/ask admin provider <anthropic\|openai\|gemini\|openrouter>` | Đổi provider AI đang dùng |
| `/ask admin setkey <provider> <key>` | Đặt API key cho 1 provider |
| `/ask admin setmodel <provider> <model>` | Đổi model của 1 provider |
| `/ask admin cooldown <giây>` | Đổi thời gian chờ giữa 2 lần hỏi |
| `/ask admin permission <0-4>` | Đổi permission level cần để dùng `/ask admin` (mặc định 2) |
| `/ask admin reload` | Đọc lại `config/askmod.json` từ đĩa |
| `/ask admin show` | Xem cấu hình hiện tại (API key được **ẩn bớt**, chỉ hiện 4 ký tự cuối) |

Ví dụ:
```
/ask admin provider openrouter
/ask admin setkey openrouter sk-or-v1-xxxxxxxxxxxxxxxx
/ask admin setmodel openrouter anthropic/claude-3.5-sonnet
/ask admin show
```

> **Lưu ý bảo mật quan trọng:** khi gõ `/ask admin setkey ...`, API key sẽ đi qua **lịch sử chat và log server** giống mọi lệnh khác — nếu server có plugin log chat hoặc người khác đứng cạnh xem chat, key có thể bị lộ. An toàn nhất là chỉ chủ server/console (qua RCON hoặc console server, không phải chat trong game) mới nên chạy lệnh `setkey`, hoặc điền key trực tiếp vào file `config/askmod.json` (Cách A ở mục 3) rồi `reload`.

## 6. Lưu ý quan trọng

- **Chi phí API**: mỗi lần `/ask` là một lần gọi API tính phí theo tài khoản của bạn (trừ khi bạn dùng model miễn phí trên OpenRouter/Gemini free tier). Cân nhắc tăng `cooldownSeconds` nếu server đông người chơi.
- **Bảo mật key**: không public file `config/askmod.json` hay đẩy nó lên GitHub vì nó chứa API key thật.
- **Không public file cấu hình**: nếu dùng Git, thêm `config/askmod.json` vào `.gitignore`.
- Mod chạy phía server nên hoạt động trên cả server dedicated lẫn singleplayer (vì singleplayer cũng chạy một integrated server). Trên singleplayer, chủ thế giới mặc định đã có permission level 4 nên luôn dùng được `/ask admin`.
