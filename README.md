# Title: VolunteerHub

## Devlogs:
how to run:
client:
``` 
    npm install
    npm run dev
```

backend: 
```
   mvn spring-boot:run
```   
database: use .sql file to create sample database

Frontend: http://localhost:5173

Backend : http://localhost:5000

##
Live Site : 
-[VolunteerHub](https://volunteer-management-sys-66dad.web.app)

Project Overview:
VolunteerHub is a volunteer management system where users can post and apply for volunteer opportunities. Users must log in with Google to view detailed event information and manage their posts and applications. This platform facilitates easy volunteer recruitment and management, streamlining the process of organizing and participating in volunteer activities. It solves the problem of fragmented volunteer coordination by centralizing postings, applications, and management in one accessible platform.

Features and Characteristics :
- A user can post his volunteer post and can hire people.
- A user also can apply for different posts where he or she wants to be a volunteer.
- Advanced filtering with search functionality features in all volunteer page to easily get the desired post.


Used Technology:
- Frontend: HTML,CSS3,TailwindCSS,Material Tailwind,React,React Router.
- Backend: NodeJs,MongoDb,ExpressJs

**Installation** :
------Client-Side------

**Cloning the Repository**
```bash
git clone https://github.com/nahidbinwadood/Volunteer-Management-System.git
cd client-side
```

Install the project dependencies using npm:

```bash
npm install
```

**Running the Client-side**

```bash
npm run dev
```

------Server Side-------
```bash
cd server-side
```

Install the project dependencies using npm:

```bash
npm install
```

**Running the Server-side**

```bash
nodemonindex.js
```
Resources:
- [React Router](https://reactrouter.com/en/main)
- [React-simple-typewriter](https://www.npmjs.com/package/react-simple-typewriter)
- [React Tooltip](https://react-tooltip.com/)
- [React-Hot-Toast](https://react-hot-toast.com/)
- [React-AOS-Package](https://michalsnik.github.io/aos/)
- [Animate.css](https://animate.style/)
- [React Hook Form](https://react-hook-form.com/)
- [React Helmet](https://www.npmjs.com/package/react-helmet-async)
- [React Router Dom](https://reactrouter.com/en/main)
- [TailwindCSS Buttons](https://devdojo.com/tailwindcss/buttons)
- [Mamba UI - Components](https://mambaui.com/components)
- [Animated Gradient Text](https://www.andrealves.dev/blog/how-to-make-an-animated-gradient-text-with-tailwindcss/)
- [React-Hot-Toast](https://react-hot-toast.com/)
- [React-Spinner](https://www.npmjs.com/package/react-spinners)
- [React-Icons](https://react-icons.github.io/react-icons/)
- [React-Markdown](https://www.npmjs.com/package/react-markdown)
- [Prop-Types](https://www.npmjs.com/package/prop-types)
- [Daisy UI](https://daisyui.com/)
- [TailwindCSS](https://tailwindcss.com/)

## Async Volunteer Request (RabbitMQ - Spring Boot Backend)
Hệ thống đã chuyển endpoint `POST /request-volunteer` sang bất đồng bộ sử dụng RabbitMQ.

Luồng mới:
1. Client gửi request với `volunteerPost.id` và `suggestion`.
2. Backend trả về `202 Accepted` kèm `trackingId` và trạng thái `QUEUED` rất nhanh (không chờ ghi DB).
3. Message được đưa vào queue `volunteer.request.queue` (exchange `volunteer.request.exchange`).
4. Consumer lấy message, kiểm tra tính hợp lệ và ghi bản ghi vào MySQL sau đó cache Redis được evict cho email volunteer.
5. Client có thể gọi lại `GET /get-volunteer-request/{email}` để thấy yêu cầu sau một độ trễ nhỏ.

Cấu hình RabbitMQ (mặc định):
```
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

Chạy RabbitMQ nhanh bằng Docker:
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
UI quản trị: http://localhost:15672 (guest/guest).

Lợi ích:
- Giảm độ trễ phản hồi API.
- Tránh nghẽn khi nhiều người dùng gửi request cùng lúc.
- Tăng khả năng chịu tải nhờ hàng đợi đệm.
- Không phụ thuộc vào thời gian xử lý ghi DB trực tiếp.

