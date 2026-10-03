# S6-08 — Backend xác nhận giao hàng thành công kèm ảnh chứng từ

## Phạm vi

Triển khai theo kiến trúc `Servlet → Service → DAO → Model`, dùng MySQL/JDBC và `PreparedStatement`.

## Luồng nghiệp vụ

- `READY` → `DELIVERED`: bắt buộc có người nhận và ảnh JPG/PNG hợp lệ. `delivered_at` và `invoice_eligible_at` lấy theo thời gian server.
- `READY` → `FAILED_RETURNED`: bắt buộc có lý do. Với mỗi mặt hàng trong phiếu, tạo một `inventory_movements` loại `SHIPMENT_RETURN` gắn với kho và phiếu giao.
- Phiếu đã chốt không được xác nhận lại.
- Nhân viên kho / Quản lý kho chỉ được thao tác phiếu thuộc kho được gán; Admin được thao tác toàn hệ thống.
- Ảnh chứng từ lưu ngoài thư mục deploy của Tomcat, DB chỉ lưu đường dẫn tương đối.

## Endpoint

`GET /shipments/confirm?id={id}` — mở phiếu cần xác nhận.

`POST /shipments/confirm` — xác nhận với:
- `outcome=SUCCESS`, `recipientName`, `proofImage`
- hoặc `outcome=FAILED`, `failureReason`

Endpoint được bảo vệ bởi `Module.SHIPMENT` và CSRF của hệ thống.

## Dữ liệu

Migration: `database/migrations/008_shipment_delivery_confirmation.sql`

- `shipments`
- `shipment_items`
- `inventory_movements`

Sprint 7 có thể dùng `invoice_eligible_at IS NOT NULL` kết hợp trạng thái `DELIVERED` làm điều kiện trước khi phát hành hoá đơn.

## Kiểm thử

Đã thêm `ShipmentServiceTest` kiểm tra:
- giao thành công + ảnh chứng từ + mở điều kiện hoá đơn;
- giao thất bại + lý do + tạo movement quay kho;
- thiếu dữ liệu bắt buộc;
- giới hạn theo kho và không cho chốt lại.

Môi trường hiện tại không có Maven/H2 nên chưa chạy được `mvn test`; phần Java mới đã được kiểm tra biên dịch cú pháp bằng `javac`, gồm cả Servlet với stub Jakarta tối thiểu.
