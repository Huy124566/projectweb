// Biến lưu trữ danh sách ghế đang chọn
let selectedSeats = [];

/**
 * 1. Tải danh sách ghế từ Backend khi trang web vừa mở
 */
async function loadSeats() {
    try {
        const response = await fetch('/api/seats');
        const seats = await response.json();
        renderSeatMap(seats);
    } catch (error) {
        console.error("Lỗi khi kết nối server:", error);
        showToast("Không thể tải danh sách ghế!", false);
    }
}

/**
 * 2. Vẽ sơ đồ ghế lên màn hình (DOM Manipulation)
 */
function renderSeatMap(seats) {
    const seatMap = document.getElementById('seat-map');
    seatMap.innerHTML = ''; // Xóa trắng sơ đồ cũ để vẽ mới

    seats.forEach(seat => {
        const div = document.createElement('div');
        div.classList.add('seat', seat.status);
        
        // Phân vùng VIP cho hàng A và B (Concert Vibe)
        if (seat.seatCode.startsWith('A') || seat.seatCode.startsWith('B')) {
            div.classList.add('VIP');
        }

        div.innerText = seat.seatCode;
        
        // Chỉ cho phép click nếu ghế đang trống (AVAILABLE)
        if (seat.status === 'AVAILABLE') {
            div.onclick = () => toggleSeat(div, seat);
        }
        
        seatMap.appendChild(div);
    });
}

/**
 * 3. Xử lý khi người dùng click vào một ghế
 */
function toggleSeat(element, seat) {
    if (element.classList.contains('SELECTED')) {
        // Nếu đã chọn rồi thì bỏ chọn
        element.classList.remove('SELECTED');
        selectedSeats = selectedSeats.filter(s => s.id !== seat.id);
    } else {
        // Nếu chưa chọn thì thêm vào danh sách
        element.classList.add('SELECTED');
        selectedSeats.push(seat);
    }
    updateUI();
}

/**
 * 4. Cập nhật chữ hiển thị mã ghế và tổng tiền
 */
function updateUI() {
    const textElement = document.getElementById('selected-seats-text');
    const priceElement = document.getElementById('total-price');

    const text = selectedSeats.map(s => s.seatCode).join(', ') || 'Chưa có ghế nào';
    const total = selectedSeats.reduce((sum, s) => sum + (s.price || 0), 0);
    
    textElement.innerText = text;
    priceElement.innerText = total.toLocaleString();
}

/**
 * 5. Gửi yêu cầu đặt vé về Backend (API POST)
 */
async function confirmBooking() {
    // Kiểm tra nếu chưa chọn ghế nào
    if (selectedSeats.length === 0) {
        showToast("Vui lòng chọn ít nhất 1 ghế!", false);
        return;
    }

    const seatIds = selectedSeats.map(s => s.id);

    try {
        const response = await fetch('/api/seats/book', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(seatIds)
        });

        const result = await response.text();

        if (response.ok) {
            showToast("🎉 Đặt vé thành công!", true);
            // Reset lại giao diện sau khi đặt thành công
            selectedSeats = [];
            updateUI();
            loadSeats(); // Tải lại sơ đồ để cập nhật những ghế vừa mua sang màu đỏ
        } else {
            showToast("Lỗi: " + result, false);
        }
    } catch (error) {
        console.error("Lỗi đặt vé:", error);
        showToast("Lỗi kết nối server!", false);
    }
}

/**
 * 6. Hàm hiển thị thông báo nổi (Toast Notification)
 */
function showToast(message, isSuccess = true) {
    const toastElement = document.getElementById('liveToast');
    const toastMessage = document.getElementById('toast-message');
    
    // Đổi màu nền dựa trên trạng thái (Thành công = Xanh, Thất bại = Đỏ)
    toastElement.classList.remove('bg-success', 'bg-danger');
    toastElement.classList.add(isSuccess ? 'bg-success' : 'bg-danger');
    
    toastMessage.innerText = message;
    
    // Sử dụng thư viện Bootstrap để hiển thị Toast
    // Đảm bảo bạn đã nạp bootstrap.bundle.min.js trong file HTML
    if (typeof bootstrap !== 'undefined') {
        const toast = new bootstrap.Toast(toastElement);
        toast.show();
    } else {
        // Nếu thiếu thư viện thì quay về dùng alert truyền thống
        alert(message);
    }
}

// Tự động chạy khi trang web tải xong
document.addEventListener('DOMContentLoaded', loadSeats);