# ecommerce-api
Membuat api e-commerce sederhana

Program ini merupakan API untuk aplikasi e-commerce yang memungkinkan mengelola produk, pengguna, pesanan, dan ulasan. API ini dibangun dengan menggunakan Java dan SQLite sebagai database.

## Setup

1. Pastikan Anda telah menginstal Java Development Kit (JDK) dan IntelliJ IDEA di komputer Anda.

2. Clone repository ini ke dalam direktori lokal.

3. Buka proyek dalam IntelliJ IDEA.

4. Pastikan Anda telah menginstal pustaka "dotenv-java" dengan mengunduh JAR file dari [dotenv-java di Maven Central](https://search.maven.org/artifact/io.github.cdimascio/dotenv-java) dan menambahkannya ke direktori "lib" dalam proyek Anda. Selain itu instal juga pustaka jdbc-sqlte dab json dalam JAR file

5. Buatlah file `.env` di direktori proyek Anda dan berikan nilai untuk variabel lingkungan yang dibutuhkan. Contoh: API_KEY=your-api-key


## Menjalankan API

1. Pastikan Anda telah mengatur variabel lingkungan yang diperlukan dalam file `.env`.

2. Jalankan aplikasi dari IntelliJ IDEA dengan menjalankan class `Main`.

3. API akan berjalan di `http://localhost:003`. Dan dapat dicoba menggunakan aplikasi Postman. Ingat untuk menambahkan headers, dengan Key = x-api-key dan Value nya sesuai variabel API_KEY pada file .env.

## Endpoints

### Produk

- **GET /products**: Mendapatkan semua produk.
- **GET /products?field=stock&cond=equal&val=5**: Memfilter produk yang memiliki stock = 5. field, cond, dan val nya dapat diubah sesuai dengan kebutuhan dan sesuai dengan cond yang sudah ditentukan pada kode program.
- **GET /products/1**: Mendapatkan produk berdasarkan ID. Angka 1 dapat diubah sesuai dengan id products yang tersedia.
- **POST /products**: Membuat produk baru. Jangan lupa tambahkan body raw json nya, contohnya sebagai berikut:
{
  "seller": "4",
  "price": "18000000",
  "description": "Air Jordan 1 Travis Scott Size 45 BNIB",
  "title": "Sepatu Air Jordan",
  "stock": "5"
}
- **PUT /products/1**: Mengubah informasi produk yang memiliki id products = 1. Angka 1 dapat dirubah sesuai kebutuhan. Jangan lupa tambahkan body raw json nya, contohnya sebagai berikut:
{
  "seller": "2",
  "price": "18000000",
  "description": "Air Jordan 1 Travis Scott Size 45 BNIB",
  "title": "Sepatu Air Jordan Asli",
  "stock": "5"
}
- **DELETE /products/1**: Menghapus produk yang memiliki products = 1. Angka 1 dapat dirubah sesuai kebutuhan.

### Pengguna

- **GET /users**: Mendapatkan semua pengguna.
- **GET /users?type=seller**: Memfilter user yang memiliki type = seller.
- **GET /users/{id}**: Mendapatkan pengguna berdasarkan ID dan menampilkan alamatnya.
- **POST /users**: Membuat pengguna baru. Jangan lupa tambahkan body raw json nya, contohnya sebagai berikut:
{
    "last_name": "GGG",
    "phone_number": "081236785394",
    "type": "seller",
    "first_name": "Bagus",
    "email": "bagusrnb12@gmail.com"
 }
- **POST /users/addresses**: Membuat alamat baru pengguna. Jangan lupa tambahkan body raw json nya, contohnya sebagai berikut:
{
    "user_id": 3,
    "province": "Bali",
    "city": "Gianyar",
    "address_id": "1",
    "postcode": "80571",
    "type": "home",
    "line2": "Kedewatan, Ubud",
    "line1": "Jl. Raya Bunutan No. 7"
}
- **PUT /users/{id}**: Mengubah informasi pengguna. Jangan lupa tambahkan body raw json nya, contohnya sebagai berikut:
{
    "last_name": "GGG",
    "phone_number": "081236785394",
    "type": "seller",
    "first_name": "Bagus",
    "email": "bagusrnb12@gmail.com"
 }
- **PUT /users/addresses/{address_id}**: Mengubah informasi address pengguna. Jangan lupa tambahkan body raw json nya, contohnya sebagai berikut:
{
    "province": "Bali",
    "city": "Klungkung",
    "address_id": "1",
    "postcode": "80571",
    "type": "home",
    "line2": "Banjarangkan",
    "line1": "Jl. Raya Getakan No. 7"
}
- **DELETE /users/{id}**: Menghapus user berdasarkan id.
- **DELETE /users/addresses/{address_id}**: Menghapus alamat pengguna berdasarkan address id.

### Pesanan

- **GET /orders**: Mendapatkan semua pesanan.
- **GET /orders?is_paid=TRUE**: Memfilter orders yang memiliki is_paid = TRUE. Dapat juga mengganti TRUE dengan FALSE.
- **GET /orders/{id}**: Mendapatkan pesanan berdasarkan ID.
- **POST /orders**: Membuat pesanan baru. Jangan lupa tambahkan body raw json nya, contohnya sebagai berikut:
{
  "buyer": 1,
  "note": "Packing rapi",
  "discount": 100000,
  "is_paid": "TRUE",
  "order_items": [
    {
      "product_id": 1,
      "quantity": 2
    },
    {
      "product_id": 2,
      "quantity": 3
    }
  ]
}
- **DELETE /orders/{id}**: Menghapus order berdasarkan id.

### Ulasan

- **GET /reviews**: Mendapatkan semua ulasan.
- **GET /reviews?star=5**: Mendapatkan ulasan berdasarkan rating bintang. Dapat menganti angka 5 dengan angka 1-5.
- **GET /reviews/order/{order_id}**: Mendapatkan ulasan berdasarkan ID pesanan.
- **POST /reviews/order/{order_id}**: Membuat ulasan untuk pesanan. Jangan lupa tambahkan body raw json nya, contohnya sebagai berikut:
{
  "star": 5,
  "description": "Barang sesuai gambar dan packing aman"
}
- **DELETE /reviews/order/{order_id}**: Menghapus ulasan berdasarkan ID pesanan.
