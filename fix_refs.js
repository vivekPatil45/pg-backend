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

            // Fix remaining stale references
            content = content.replace(/ReservationStatus/g, 'BookingStatus');
            content = content.replace(/\.reservationId/g, '.bookingId');
            content = content.replace(/"reservationId"/g, '"bookingId"');
            content = content.replace(/customerId/g, 'tenantId');
            content = content.replace(/CustomerRepository/g, 'TenantRepository');
            content = content.replace(/customerRepository/g, 'tenantRepository');
            content = content.replace(/ReservationRepository/g, 'BookingRepository');
            content = content.replace(/reservationRepository/g, 'bookingRepository');
            content = content.replace(/CustomerService/g, 'TenantService');
            content = content.replace(/customerService/g, 'tenantService');
            content = content.replace(/TenantController\b/g, 'TenantController');
            content = content.replace(/pricePerNight/g, 'price');
            content = content.replace(/searchCustomers/g, 'searchTenants');
            content = content.replace(/searchReservations/g, 'searchBookings');

            // enums.ReservationStatus import fix
            content = content.replace(/com\.pg\.enums\.ReservationStatus/g, 'com.pg.enums.BookingStatus');

            fs.writeFileSync(fullPath, content);
        }
    }
}

processDir('c:/Users/vivek/OneDrive/Desktop/HMS/pg-backend/src/main/java/com/pg');
console.log('Stale references replaced.');
