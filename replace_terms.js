const fs = require('fs');
const path = require('path');

function processDir(dir) {
    const items = fs.readdirSync(dir);
    for (const item of items) {
        const fullPath = path.join(dir, item);
        if (fs.statSync(fullPath).isDirectory()) {
            processDir(fullPath);
        } else if (fullPath.endsWith('.java')) {
            let content = fs.readFileSync(fullPath, 'utf8');

            // Perform replacements
            content = content.replace(/\bCustomer\b/g, 'Tenant');
            content = content.replace(/\bcustomer\b/g, 'tenant');
            content = content.replace(/\bCustomers\b/g, 'Tenants');
            content = content.replace(/\bcustomers\b/g, 'tenants');
            content = content.replace(/\bCUSTOMER\b/g, 'TENANT');

            content = content.replace(/\bReservation\b/g, 'Booking');
            content = content.replace(/\breservation\b/g, 'booking');
            content = content.replace(/\bReservations\b/g, 'Bookings');
            content = content.replace(/\breservations\b/g, 'bookings');
            content = content.replace(/\bRESERVATION\b/g, 'BOOKING');

            content = content.replace(/\bfullName\b/g, 'name');
            content = content.replace(/\bmobileNumber\b/g, 'phone');

            // We will handle moveInDate manually or bulk replace checkInDate
            content = content.replace(/\bcheckInDate\b/g, 'moveInDate');
            content = content.replace(/\bcheckOutDate\b/g, 'moveOutDate');

            fs.writeFileSync(fullPath, content);
        }
    }
}

processDir('c:/Users/vivek/OneDrive/Desktop/HMS/pg-backend/src/main/java/com/pg');
console.log('Replacements completed.');
