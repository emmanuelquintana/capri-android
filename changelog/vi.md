# Changelog (Tiếng Việt)

Giống CHANGELOG.md (tiếng Tây Ban Nha, bản gốc), bằng tiếng Việt: ứng dụng hiển thị nó khi đang ở tiếng Việt.

## 1.12.0

- **Lại chạy trên điện thoại 32-bit:** từ bản 1.11.0 ứng dụng không cài được trên điện thoại 32-bit (armeabi-v7a, như
  TECNO Spark Go 1S và nhiều máy Android Go): trình cài đặt báo bộ xử lý không tương thích. Giờ cùng một APK cài được
  trên mọi máy. Trên những máy đó, giọng của cô ấy cho câu trả lời (công cụ) không có, nhưng câu thu âm và giọng điện
  thoại thì có.
- **Giọng cô ấy trả lời nhanh hơn nhiều:**
  - Trong lúc nói một câu, cô ấy đã tạo câu tiếp theo. Trước đây phải nói xong mới bắt đầu câu sau, nên câu trả lời
    dài mất gấp đôi thời gian.
  - Cô ấy bắt đầu nói ngay khi có phần đầu của câu, không chờ cả câu.
  - Công cụ bắt đầu tải ngay khi bạn viết hoặc nói với cô ấy, trong lúc AI suy nghĩ, chứ không phải ở cuối.
  - Không còn bị giải phóng khỏi bộ nhớ mỗi lần bạn rời ứng dụng, và ở lại lâu hơn trên máy nhiều RAM (tới 15 phút).
  - Dùng các nhân nhanh của bộ xử lý (tới 6) trên máy có chúng.
  - Nhớ những câu đã nói, nên câu lặp lại phát ra ngay.
- **Phần trăm giọng nói trong chat:** câu trả lời hiện bằng chữ ngay lập tức, bên dưới thấy giọng đã tạo được bao nhiêu
  (*Đang tạo giọng 42%*), rồi *Đang nói*. Trong tab Chat và trong bong bóng của cô ấy.
- **Không còn im lặng:** nếu bạn chạm hay di chuyển cô ấy khi đang chuẩn bị giọng, câu nói khi được vuốt ve làm hủy cả
  câu trả lời. Giờ câu trả lời được ưu tiên. Và trong lúc chuẩn bị giọng, cô ấy vẫn nghe bạn.
- **Câu thu âm thường xuyên hơn:** khi vuốt ve, di chuyển, ném cô ấy, và giờ cả khi chào bạn, khi chán, khi mở và
  đóng chat, khi mất mạng, khi bạn chơi game và khi nhắc nhở.
- **Nói trong lúc AI còn đang viết:** ngay khi AI viết xong câu đầu tiên, giọng cô ấy bắt đầu nói trong lúc phần còn
  lại được viết. Trong tab Chat bạn thấy câu trả lời được viết trực tiếp.
- **Công cụ sẵn sàng ngay:** khi tải, nó khởi động nhẹ trong im lặng để câu trả lời đầu tiên không bị chậm.
- **Bốn ngôn ngữ mới: tiếng Việt, tiếng Nhật, tiếng Đức và tiếng Ý.** Toàn bộ ứng dụng, câu nói của cô ấy, trò chơi,
  chế độ ngủ, tủ đồ, lệnh giọng nói («Capri, …») và câu trả lời của AI. Chọn trong Cài đặt › Ngôn ngữ. Giọng riêng
  của cô ấy cho câu trả lời hiện chỉ có tiếng Tây Ban Nha và tiếng Anh; bằng tiếng Việt cô ấy dùng giọng điện thoại.
- **Không còn bị kẹt khi đang nghĩ trong chat:** nếu bạn nhắn khi cô ấy vẫn đang trả lời, cô ấy nói «Chờ mình một
  giây» rồi im luôn. Giờ mỗi tin nhắn chờ đến lượt và nhận câu trả lời riêng. Nếu AI dừng giữa chừng, cô ấy giữ
  phần đã viết.
- **AI miễn phí không còn tự vướng:** nó chỉ nhận một yêu cầu mỗi lần trên mỗi điện thoại. Ghi chú về bạn và
  neurodance giờ chờ tin nhắn của bạn xong (và nhường nếu bạn nhắn).
- **Nhiều khóa API:** trong màn hình AI của cô ấy, bạn có thể dán nhiều khóa của cùng một dịch vụ, cách nhau bằng
  dấu phẩy, hoặc thêm khóa khác sau. Khi một khóa hết hạn mức (như Gemini miễn phí), cô ấy chuyển ngay sang khóa
  tiếp theo.
- **Cô ấy báo tin nhắn cho bạn:** thỉnh thoảng, khi có thông báo, cô ấy nói bằng giọng đã thu âm.
- **Giọng cô ấy bắt đầu sớm hơn nữa:** nếu câu đầu dài, cô ấy nói phần đầu (đến dấu phẩy) trước.
- **Nghe thụ động, như «Hey Siri»:** trước đây cô ấy dùng trình nhận dạng giọng nói của điện thoại, vốn chỉ nghe vài
  giây mỗi lần: micro bật tắt cả ngày kèm tiếng bíp. Giờ một mô hình nhỏ ngay trong điện thoại liên tục chỉ nghe tên
  cô ấy: không bíp, micro không nhấp nháy, không gì rời khỏi điện thoại. Tải một lần cho mỗi ngôn ngữ (khoảng 40 MB)
  qua Wi-Fi, hoặc trong **Cài đặt › Cài đặt giọng nói › Nghe thụ động**.
- **Cô ấy thức dậy bằng giọng của mình:** nếu đang ngủ và bạn gọi tên, cô ấy thức dậy và nói bằng câu đã thu âm.
- **Giọng nhanh hơn trên điện thoại mạnh:** với RAM từ 8 GB, bộ máy không còn gỡ và nạp lại các phần của mô hình sau
  mỗi câu. Câu đầu ngắn hơn và giọng bắt đầu tải ngay khi bạn mở chat.
- **Không còn dừng sau câu đầu:** cô ấy nói tiếp phần còn lại.
- **AI nào trả lời, ngay trong chat:** ở đầu tab Chat bạn thấy AI nào trả lời và đổi bằng một chạm.
- **Câu báo tin nhắn và thông báo thật sự phát ra:** thông báo đầu tiên luôn được báo, kể cả khi đang mở ứng dụng.
- **Menu gọn hơn:** Cộng đồng và Lệnh giọng nói giờ là một tab.
- **Gemini nhanh trở lại:** Google đã đóng `gemini-2.5-flash` với khóa mới mà ứng dụng vẫn thử nó đầu tiên ở mỗi
  tin nhắn. Giờ mô hình đã bị đóng sẽ không được dùng lại và chuyển thẳng sang mô hình Google gợi ý.
- **Spotify và độ chính xác giọng nói được lưu:** khi Capri đang bật, chọn Spotify hoặc độ chính xác trong cài đặt
  giọng nói không được lưu. Đã sửa.
- **Trò chơi:** đổi độ khó ngay trong từng trò (nút ◆), kể cả khi vào từ tab Trò chơi. Ở Connect 4 hai bên lần
  lượt đi trước.
- **Màn hình toàn phần gọn gàng:** trong Tủ đồ và trò chơi, chữ không còn đè lên giờ và pin, hình của cô ấy ở góc
  không còn bị nút điều hướng che.
- **Cô ấy không còn hiện đè lên trò chơi** khi trang của cô ấy khởi động lại lúc bạn đang chơi.
- **Tủ đồ:** sửa lỗi khiến danh sách Phụ kiện trống.
- **Xác nhận trước khi xóa ký ức của cô ấy**, và **sửa lớp học** bằng cách chạm vào.
- **Cô ấy hiểu tên mình dù nói hơi khác** («Capi», «Cabi»…), và nếu bạn nhờ mà không gọi tên, thỉnh thoảng cô ấy
  nhắc: «Gọi tên mình trước».
- **Tài khoản của bạn:** đăng nhập bằng Google hoặc email (trong «Bạn») và ký ức của cô ấy về bạn, cài đặt, lịch học và
  cuộc trò chuyện được lưu lên đám mây. Trên điện thoại mới, cô ấy vẫn nhớ bạn. Khóa AI không bao giờ rời điện thoại.
- **Chế độ đọc:** nói «tôi đang đọc Dune» và «tôi đang ở trang 120», rồi hỏi tóm tắt hoặc về nhân vật: cô ấy chỉ trả
  lời đến trang của bạn, không spoil.
- **Tệp cho cô ấy:** chia sẻ PDF, Word, văn bản hoặc âm thanh (hoặc 📎 trong chat) và cô ấy đọc hoặc nghe cùng bạn.
- **«Capri, bài gì đây?»:** cô ấy nghe vài giây và cho bạn biết (cần khóa Gemini).
- **Trả lời tin nhắn:** «Capri, trả lời Ana là mình tới liền» trả lời từ thông báo.
- **Hai widget mới:** «Capri mini» và «Ngày của bạn với Capri».
- **Tin tức hôm nay:** «Capri, có gì mới?» và cô ấy đọc các tiêu đề.
- **Lắc điện thoại** và cô ấy chóng mặt (tắt được trong Cài đặt).
- **Chờ trước khi chụp màn hình:** 0, 2, 4 hoặc 6 giây, trong Chế độ.
- **Tìm trong chat:** nút ⌕ ở thẻ Chat.
- **Hết tay đôi:** vài bộ đồ vẽ thêm một cánh tay đứng yên. Đã sửa.
- **Không còn lẫn trang phục khi hoạt ảnh:** khi mặc đồ khác, cô ấy không đổi về đồ thường giữa hoạt ảnh nữa.
  Chỉ chạy hoạt ảnh được vẽ cho bộ đồ đó.
- **Skin cộng đồng:** trong Tùy chỉnh › «Tạo Capri của riêng bạn», tải mẫu của cô ấy, vẽ trang phục và gửi đi. Nếu Emma
  và server Discord duyệt, nó sẽ có trong bản cập nhật kèm tên bạn.
- **Giọng thật của cô ấy:** có bộ máy giọng nhưng thiếu hồ sơ thì cô ấy dùng giọng điện thoại. Giờ hồ sơ tải cùng bộ
  máy, và nếu thiếu cô ấy tự tải.
- **Gõ trong ứng dụng:** «Capri, viết ở đây …» khi đang mở chat không còn làm mất bàn phím.
- **Chỉ giọng của bạn:** «Chỉ giọng của tôi» học giọng bạn 3 lần đầu, sau đó bỏ qua tên cô ấy nói bằng giọng khác.
  Mọi thứ ở trên điện thoại.
- **Menu «AI nào trả lời cô ấy?» theo phong cách của cô ấy:** bộ chọn AI trong chat giờ giống phần còn lại của app.
- **Neurodance có âm thanh:** giờ có không khí (tiếng ù, nhiễu, glitch) và cô ấy kể bằng giọng của mình.
- **Kéo cô ấy lại được:** trên một số điện thoại (Xiaomi) app tưởng bàn phím vẫn mở và không cho di chuyển. Đã sửa.
- **Tải giọng cô ấy không lỗi:** nếu mất kết nối, việc tải tiếp tục từ chỗ đang dở.
- **Giọng cô ấy không còn bị cắt giữa chừng:** câu trả lời dài được nói hết, cả với giọng điện thoại.
- **Ba minigame mới:** Phản xạ, Đoán số và Lớn hay nhỏ, trong tab Trò chơi.
- **Chọn giọng điện thoại:** trong Cài đặt giọng › Giọng điện thoại, nghe các giọng của máy và chọn giọng giống cô ấy
  nhất, cho lúc giọng riêng của cô ấy không có.
- **Cô ấy không còn im lặng:** nếu bộ máy giọng bị kẹt, cô ấy im ở mọi câu sau; giờ cô ấy tạm trả lời bằng giọng điện
  thoại. Và nếu AI lỗi giữa câu trả lời, cô ấy không còn đứng ở «Đang nói · 90 %» cả phút.
- **Giọng cô ấy trên đám mây (Fish Audio):** trong Cài đặt giọng, thêm khóa Fish Audio của bạn và cô ấy nói bằng giọng
  của mình theo thời gian thực ở mỗi câu trả lời, bắt đầu sau nửa giây khi AI vẫn đang viết. Khóa chỉ ở trên điện thoại
  của bạn. Không có mạng hoặc hết tiền, cô ấy tự quay về giọng điện thoại.
- **Câu nói của cô ấy bằng giọng mới:** 202 câu đã ghi âm (tiếng Tây Ban Nha và tiếng Anh) giờ dùng giọng Fish Audio
  của cô ấy. Vẫn phát ngay và không cần mạng. Nếu bạn đã có giọng cô ấy, bản mới sẽ tự tải về.
- **Cài đặt giọng gọn hơn:** giờ là một menu ngắn. Ở trên bạn bật hoặc tắt giọng cô ấy, mỗi thứ có màn hình riêng:
  Tải giọng cô ấy, Giọng trên đám mây (khóa của bạn), Giọng cô ấy nghe thế nào và Đôi tai của cô ấy. Trước Cài đặt nâng
  cao có công tắc mới **Giọng: đám mây hay cục bộ**.
- **Chat trả lời nhanh hơn:** khi AI của cô ấy quá tải, AI đã trả lời được dùng trước trong vài phút.
- **Câu đã ghi âm cũng có trong chat:** trong lúc nghĩ câu trả lời, thỉnh thoảng cô ấy nói một câu của mình.
