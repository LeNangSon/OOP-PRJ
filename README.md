# OOP-PRJ - Ecosystem Simulation

Dự án mô phỏng hệ sinh thái bằng JavaFX cho môn Lập trình hướng đối tượng. Ứng dụng hiển thị một bản đồ 2D với các loài động vật, thực vật, địa hình và chuỗi thức ăn. Một số hành vi có thể chạy bằng luật cố định, Q-learning hoặc Deep Q-learning.

## Tính năng chính

- Mô phỏng các thực thể sống: thỏ, sói, gấu, voi, cá.
- Mô phỏng thực thể tĩnh và tài nguyên: cỏ, tảo, bụi cây, đá.
- Bản đồ 4 mùa, tự chuyển mùa sau một khoảng thời gian và cho phép đổi mùa thủ công.
- Hệ thống nhật ký sự kiện: sinh ra, chết, không thể đặt vật thể, trạng thái hệ thống.
- Bảng thống kê trạng thái thực thể trong sidebar.
- Điều khiển đặt thêm sinh vật/vật thể trực tiếp trên bản đồ.
- Phóng to, thu nhỏ và kéo bản đồ khi đang zoom.
- Hỗ trợ Q-learning dạng bảng và Deep Q-learning cho hành vi săn mồi của sói.

## Công nghệ sử dụng

- Java
- JavaFX 21.0.6
- Maven
- JUnit 5

Project đang cấu hình compiler với Java `25` trong `pom.xml`, vì vậy cần JDK tương thích với cấu hình này hoặc chỉnh lại `source/target` nếu máy chỉ có JDK thấp hơn.

## Cấu trúc thư mục

```text
.
├── pom.xml
├── mvnw / mvnw.cmd
├── train.sh
├── train_dqn.sh
├── qtables/
├── src/main/java/org/openjfx/app/
│   ├── Launcher.java
│   ├── MainApp.java
│   ├── EntityStatusPanel.java
│   ├── core/
│   └── entities/
└── src/main/resources/org/openjfx/app/
    ├── all.tmx
    ├── ui.css
    ├── spring.jpg
    ├── summer.png
    ├── autumn.png
    ├── winter.png
    └── ...
```

## Cách chạy ứng dụng

Chạy giao diện JavaFX:

```bash
./mvnw javafx:run
```

Nếu muốn dùng Maven đã cài sẵn:

```bash
mvn javafx:run
```

Entry point của ứng dụng là:

```text
org.openjfx.app.Launcher
```

## Điều khiển trong giao diện

- Chọn loài/vật thể trên thanh trên cùng, sau đó nhấp vào bản đồ để thả.
- Kéo chuột trên bản đồ để di chuyển khung nhìn khi đang zoom.
- Dùng nút `+`, `-`, thanh trượt hoặc nút reset để điều chỉnh zoom.
- Chọn menu mùa để đổi mùa thủ công.
- Nhấn `TAB` để chuyển giữa Nhật ký và Thống kê.
- Nhấn `ESC` để bỏ chế độ đặt hiện tại.

## Chạy với Q-learning

Huấn luyện Q-learning:

```bash
./train.sh
```

Chạy một pha huấn luyện thủ công:

```bash
./train.sh <episodes> <maxSteps> [wolf|rabbit|wolfql|rbs]
```

Ví dụ:

```bash
./train.sh 800 600 rabbit
```

Kết quả được lưu trong:

```text
qtables/wolf.qtable
qtables/rabbit.qtable
```

Chạy giao diện với Q-learning:

```bash
./mvnw javafx:run -Dql=true
```

Xem thêm chi tiết trong [QLEARNING.md](QLEARNING.md).

## Chạy với Deep Q-learning

Huấn luyện DQN cho sói:

```bash
./train_dqn.sh
```

Huấn luyện với số episode và step tùy chỉnh:

```bash
./train_dqn.sh 5000 800
```

Kết quả được lưu trong:

```text
qtables/wolf.dqn
```

Chạy giao diện với DQN:

```bash
./mvnw javafx:run -Ddqn=true
```

Nếu bật cả `-Dql=true` và `-Ddqn=true`, DQN được ưu tiên hơn Q-learning.

## Build và kiểm tra

Biên dịch project:

```bash
./mvnw compile
```

Chạy test:

```bash
./mvnw test
```

Build package:

```bash
./mvnw package
```

## Ghi chú

- Các file tài nguyên hình ảnh, âm thanh và bản đồ nằm trong `src/main/resources`.
- File `all.tmx` là bản đồ dùng chung cho các mùa.
- Thư mục `qtables` chứa dữ liệu đã huấn luyện. Có thể train tiếp từ dữ liệu cũ.
- Nếu chạy offline, cần đảm bảo các dependency Maven đã có sẵn trong local repository.
