package com.ctu.cinema;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class TicketBookingSystem {
	private static final int TOTAL_SEATS = 50;
	
	// Mảng lưu trạng thái ghế
	// false: Chưa đặt
	// true: Đã đặt
	private final boolean[] seats = new boolean[TOTAL_SEATS + 1];
	
	// Khóa đồng bộ
	private final ReentrantLock lock = new ReentrantLock();
	
	// Biến đếm thống kê đơn giản (Chưa an toàn luồng)
	private final AtomicInteger successfulBookings = new AtomicInteger(0);
	private final AtomicInteger failedBookings = new AtomicInteger(0);
    
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
	        // Kiểm tra ghế đã được đặt chưa
	        if (!seats[seatNumber]) {
	            // Giả lập độ trễ mạng 10ms
	            try {
	                Thread.sleep(10);
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	                return false;
	            }

	            // Đánh dấu ghế đã được đặt
	            seats[seatNumber] = true;

	            // Tăng số lượt thành công
	            successfulBookings.incrementAndGet();
	            System.out.println("[Thành công] " + customerName + " đã đặt thành công ghế #" + seatNumber);
	            return true;
	        } else {
	            // Tăng số lượt thất bại
	            failedBookings.incrementAndGet();
	            System.out.println("[Thất bại] " + customerName + " đặt ghế #" + seatNumber + " nhưng ghế ĐÃ BỊ CHIẾM!");
	            return false;
	        }
	    } finally {
	        // Luôn giải phóng khóa
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
    
    // Test Runner giả lập 100 khách hàng đặt vé đồng thời trên Thread Pool
    public static void main(String[] args) throws InterruptedException {
        TicketBookingSystem bookingSystem = new TicketBookingSystem();

        // Tạo Thread Pool gồm 10 luồng
        ExecutorService executor = Executors.newFixedThreadPool(10);
        System.out.println("🚀 Bắt đầu mô phỏng 100 yêu cầu đặt vé đồng thời...");

        // Tạo 100 yêu cầu đặt vé
        for (int i = 1; i <= 100; i++) {
            final int customerId = i;

            // Xác định ghế khách hàng muốn đặt
            final int targetSeat = (i % 50) + 1;
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
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // In thống kê
        System.out.println("\n========= THỐNG KÊ TỔNG KẾT (THREAD-SAFE RUN) =========");
        System.out.println("Tổng số lượt đặt thành công: " + bookingSystem.getSuccessfulBookings());
        System.out.println("Tổng số lượt đặt thất bại: " + bookingSystem.getFailedBookings());
        System.out.println("Tổng số yêu cầu đã xử lý: " + (bookingSystem.getSuccessfulBookings() + bookingSystem.getFailedBookings()));
        System.out.println("=========================================");
    }
}