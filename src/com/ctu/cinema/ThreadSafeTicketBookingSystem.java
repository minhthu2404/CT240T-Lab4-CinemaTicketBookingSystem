package com.ctu.cinema;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeTicketBookingSystem {

    // Tổng số ghế
    private static final int TOTAL_SEATS = 50;

    // Mảng lưu trạng thái ghế
    private final boolean[] seats = new boolean[TOTAL_SEATS + 1];

    // Khóa đồng bộ
    private final ReentrantLock lock = new ReentrantLock();

    // Biến thống kê an toàn luồng
    private final AtomicInteger successfulBookings =
            new AtomicInteger(0);

    private final AtomicInteger failedBookings =
            new AtomicInteger(0);

    // Phương thức đặt vé
    public boolean bookSeat(int seatNumber, String customerName) {

        // Kiểm tra ghế hợp lệ
        if (seatNumber < 1 || seatNumber > TOTAL_SEATS) {
            System.err.println("Ghế không hợp lệ: " + seatNumber);
            return false;
        }

        // Khóa miền tối hạn
        lock.lock();

        try {

            // Kiểm tra trạng thái ghế
            if (!seats[seatNumber]) {

                // Giả lập độ trễ mạng 10ms
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }

                // Đặt ghế
                seats[seatNumber] = true;

                // Cập nhật thống kê
                successfulBookings.incrementAndGet();

                System.out.println(" [Thành công] " + customerName
                        + " đã đặt thành công ghế #" + seatNumber);

                return true;

            } else {

                // Ghế đã được đặt
                failedBookings.incrementAndGet();

                System.out.println(" [Thất bại] " + customerName
                        + " đặt ghế #" + seatNumber
                        + " nhưng ghế ĐÃ BỊ CHIẾM!");

                return false;
            }

        } finally {

            // Giải phóng khóa
            lock.unlock();
        }
    }

    // Lấy số lượt đặt thành công
    public int getSuccessfulBookings() {
        return successfulBookings.get();
    }

    // Lấy số lượt đặt thất bại
    public int getFailedBookings() {
        return failedBookings.get();
    }

    // Hàm main
    public static void main(String[] args) throws InterruptedException {

    	ThreadSafeTicketBookingSystem bookingSystem =
                new ThreadSafeTicketBookingSystem();

        // Tạo Thread Pool gồm 10 luồng
        ExecutorService executor =
                Executors.newFixedThreadPool(10);

        System.out.println(
                "Bắt đầu mô phỏng 100 yêu cầu đặt vé (THREAD-SAFE)..."
        );

        // Gửi 100 yêu cầu đặt vé
        for (int i = 1; i <= 100; i++) {

            final int customerId = i;

            // Xác định ghế cần đặt
            final int targetSeat = (i % 50) + 1;

            // Đưa tác vụ vào Thread Pool
            executor.submit(() -> {
                bookingSystem.bookSeat(
                        targetSeat,
                        "Khách hàng #" + customerId
                );
            });
        }

        // Ngừng nhận tác vụ mới
        executor.shutdown();

        // Chờ các luồng hoàn thành
        boolean completed = executor.awaitTermination(
                10, TimeUnit.SECONDS
        );

        if (!completed) {
            executor.shutdownNow();
            System.out.println("Hệ thống chưa xử lý xong trong thời hạn.");
            return;
        }

        // Thống kê kết quả
        int success = bookingSystem.getSuccessfulBookings();
        int failed = bookingSystem.getFailedBookings();

        System.out.println("\n========= THỐNG KÊ TỔNG KẾT =========");

        System.out.println("Tổng số lượt đặt THÀNH CÔNG: " + success);

        System.out.println("Tổng số lượt đặt THẤT BẠI: " + failed);

        System.out.println("Tổng số yêu cầu đã xử lý: "
                + (success + failed));

        System.out.println("=====================================");

        // Kiểm tra kết quả
        if (success == 50 && failed == 50) {
            System.out.println("KẾT QUẢ: ĐẠT YÊU CẦU!");
        } else {
            System.out.println("KẾT QUẢ: CHƯA ĐẠT YÊU CẦU!");
        }
    }
}