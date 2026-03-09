const fs = require('fs');
const path = require('path');

function processDir(dir) {
    if (!fs.existsSync(dir)) return;
    const items = fs.readdirSync(dir);
    for (const item of items) {
        const fullPath = path.join(dir, item);
        if (fs.statSync(fullPath).isDirectory()) {
            processDir(fullPath);
        } else if (fullPath.endsWith('.java')) {
            let content = fs.readFileSync(fullPath, 'utf8');

            // Fix class name still named ReservationService -> BookingService (the class declaration itself)
            content = content.replace(/class ReservationService\b/g, 'class BookingService');
            content = content.replace(/\bnew ReservationService\b/g, 'new BookingService');
            content = content.replace(/ReservationService\b/g, 'BookingService');
            content = content.replace(/reservationService\b/g, 'bookingService');

            // Fix CreateReservationRequest -> CreateBookingRequest
            content = content.replace(/CreateReservationRequest\b/g, 'CreateBookingRequest');
            content = content.replace(/AdminCreateReservationRequest\b/g, 'AdminCreateBookingRequest');
            content = content.replace(/ModifyReservationRequest\b/g, 'ModifyBookingRequest');

            // Fix enums values that may be left over
            content = content.replace(/BookingStatus\.CHECKED_IN\b/g, 'BookingStatus.ACTIVE');
            content = content.replace(/BookingStatus\.CHECKED_OUT\b/g, 'BookingStatus.COMPLETED');

            // Fix field names still using old hotel terms
            content = content.replace(/\.setFullName\(/g, '.setName(');
            content = content.replace(/\.getFullName\(\)/g, '.getName()');
            content = content.replace(/\.setMobileNumber\(/g, '.setPhone(');
            content = content.replace(/\.getMobileNumber\(\)/g, '.getPhone()');
            content = content.replace(/\.setLoyaltyPoints\([^)]+\);\n/g, '\n');
            content = content.replace(/\.getLoyaltyPoints\(\)/g, '0');
            content = content.replace(/\.setCustomerId\(/g, '.setTenantId(');
            content = content.replace(/\.getCustomerId\(\)/g, '.getTenantId()');
            content = content.replace(/generateCustomerId\(/g, 'generateTenantId(');
            content = content.replace(/generateReservationId\(/g, 'generateBookingId(');
            content = content.replace(/\.setCustomer\(/g, '.setTenant(');
            content = content.replace(/\.getCustomer\(\)/g, '.getTenant()');
            content = content.replace(/\.getCheckInDate\(\)/g, '.getMoveInDate()');
            content = content.replace(/\.getCheckOutDate\(\)/g, '.getMoveOutDate()');
            content = content.replace(/\.setCheckInDate\(/g, '.setMoveInDate(');
            content = content.replace(/\.setCheckOutDate\(/g, '.setMoveOutDate(');
            content = content.replace(/setBedType\([^)]+\);\n/g, '\n');
            content = content.replace(/setViewType\([^)]+\);\n/g, '\n');
            content = content.replace(/\.setPricePerNight\(/g, '.setPrice(');
            content = content.replace(/\.getPricePerNight\(\)/g, '.getPrice()');
            content = content.replace(/setNumberOfAdults\([^)]+\);\n/g, '\n');
            content = content.replace(/setNumberOfChildren\([^)]+\);\n/g, '\n');
            content = content.replace(/setNumberOfNights\([^)]+\);\n/g, '\n');
            content = content.replace(/setBaseAmount\([^)]+\);\n/g, '\n');
            content = content.replace(/setTaxAmount\([^)]+\);\n/g, '\n');
            content = content.replace(/setDiscountAmount\([^)]+\);\n/g, '\n');
            content = content.replace(/setSpecialRequests\([^)]+\);\n/g, '\n');
            content = content.replace(/\.setReservationId\(/g, '.setBookingId(');
            content = content.replace(/\.getReservationId\(\)/g, '.getBookingId()');

            // Fix import of ReservationStatus
            content = content.replace(/import com\.pg\.enums\.ReservationStatus/g, 'import com.pg.enums.BookingStatus');

            // Fix UserRole.TENANT -> UserRole.USER (if TENANT was wrongly substituted)
            // Actually we created TENANT in Role enum? Let's fix UserRole references
            content = content.replace(/UserRole\.TENANT\b/g, 'UserRole.USER');

            fs.writeFileSync(fullPath, content);
        }
    }
}

processDir('c:/Users/vivek/OneDrive/Desktop/HMS/pg-backend/src/main/java/com/pg');
console.log('Final cleanup done.');
