# Changelog (Tiếng Việt)

Giống CHANGELOG.md (tiếng Tây Ban Nha, bản gốc), bằng tiếng Việt: ứng dụng hiển thị nó khi đang ở tiếng Việt.

## 1.11.8

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
