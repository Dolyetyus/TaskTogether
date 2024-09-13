# TaskTogether - 1.0
<p align="center">
<img src="https://github.com/user-attachments/assets/650bcaeb-1528-4721-a8ee-3c57b331dd9a" width="400"></p>

## Introduction
TaskTogether is a collaborative task management app that helps you and your partner/friend stay organized and work together efficiently. Designed with couples and close friends in mind, TaskTogether lets you create, assign, and manage tasks while staying connected with real-time updates and notifications.

Whether you're managing personal chores, work tasks, or life goals, TaskTogether makes it easy for couples, roommates, or colleagues to keep track of their shared tasks in real time.

## Features
### Registration and Authentication
- Sign up using a username, your email, and password.
- Log in safely using your username and password.
- End-to-end encryption using Google Firebase Auth services.
<div align="center">
  <img src="https://github.com/user-attachments/assets/2db44bd0-0b8f-4ae2-8198-ed6ee5eb8f53" width="250px"/> 
  <img src="https://github.com/user-attachments/assets/78a5c8f1-8a42-42a6-93fe-231781974218" width="250px"/> 
</div>

### Partner Integration
- Search for your partner by username and send partnership requests.
- Accept or reject incoming partner requests.
- View your current partner's details and remove them if needed.
<div align="center">
  <img src="https://github.com/user-attachments/assets/910e63ba-5e24-47fd-979c-f8ab67000675" width="250px"/> 
  <img src="https://github.com/user-attachments/assets/ef2f3454-ca1f-4912-a139-1cd6a59485ca" width="250px"/>
  <img src="https://github.com/user-attachments/assets/4f231197-ae54-477c-b8ea-0737f30454ca" width="250px"/>
</div>
  
### Collaborative Task Management
- Create and assign tasks to yourself, your partner, or both.
- Add task descriptions, due dates, and set task priorities.
- Real-time updates when tasks are created, updated, or completed by your partner.
<div align="center">
  <img src="https://github.com/user-attachments/assets/0945cab8-5ab9-478d-ba1f-f00bf849325b" width="250px"/> 
  <img src="https://github.com/user-attachments/assets/d6d0b6bd-d1a1-45c8-8f3d-cfd6f1d56b6c" width="250px"/>
  <img src="https://github.com/user-attachments/assets/ec6fc5d1-407a-4ba4-b94b-0831362a2b64" width="250px"/>
  <img src="https://github.com/user-attachments/assets/298f5aea-906b-4936-9647-36191fad0d14" width="250px"/>
</div>
  
### Task Sorting and Filtering
- Sort tasks by title, due date, or priority.
- Choose between ascending and descending order.
- Search tasks by title.
<div align="center">
  <img src="https://github.com/user-attachments/assets/8d00a652-fe59-437a-b52b-65313ad299c8" width="250px"/> 
  <img src="https://github.com/user-attachments/assets/ead31b60-82f5-4a81-a99a-bb6e75b535cf" width="250px"/>
</div>

### Profile Settings
- Change your username as you like (as long as the username is available).
- Delete your account completely, no need to be worried about your privacy.
<div align="center">
  <img src="https://github.com/user-attachments/assets/eb95a085-3cd8-431f-9373-b79833d9ac46" width="250px"/> 
</div>

### User-Friendly Interface
- Simple and intuitive user interface.
- Edge-to-edge UI design for a seamless experience.
<div align="center">
  <img src="https://github.com/user-attachments/assets/969640fc-6855-4c15-9829-7e610d165a64" width="250px"/> 
  <img src="https://github.com/user-attachments/assets/efed9080-db1b-4df6-9217-32026c9efeb9" width="250px"/> 
</div>

### Firebase Integration
- Secure user authentication using Firebase Authentication.
- Real-time task updates using Firebase Realtime Database.

### Planned Future Update: Task Notifications
- Get notifications for task deadlines, new tasks, and task completion status.
- Set reminders at different intervals before the task due date.
- Get daily reminders to create and complete tasks.
 Note: I could not implement these features as I intended, since the FCM and Firecloud functions are paid services, and I did not want to spend money on this side project bceuase my intention is not to use this app for commercial use. I will implement these features when I have time to find a way around and adapt them in the application.

## Technical Details

### Project Structure
TaskTogether is developed using Kotlin for Android and incorporates the following technical elements:
</br>
</br>
- **Object Oriented and Functional Programming:** Many classes that make use of object oriented and functional programming principles.
- **Firebase Authentication:** FirebaseAuth for user login and registration.</br>
- **Firebase Realtime Database:** An efficient database is designed for storing user data, tasks, and partner information in real time.</br>
- **RecyclerView & Adapters**: To dynamically display tasks with options to delete, complete, or reactivate.</br>
- **Custom Toasts:** Used for a modern user feedback throughout the app.</br>
- **Dialogs:** Utilized for task creation and confirmations.</br>
- **Edge-to-edge support:** Providing a modern, immersive UI experience.</br>
- **SearchView:** For filtering tasks based on user input.</br>

### Codebase
- **About 3000 lines of Kotlin code:** Core logic for task management, partnership features, and user interactions.</br>
- **More than 5000 lines in total:** Including layout files (XML) for activity screens, dialogs, and widgets.</br>
- **Modular Code Structure:** Separate classes for task management, partner handling, and user authentication, with a well-organized package.</br>
- **Lifecycle-aware Components:** Efficient use of lifecycle methods for seamless state management.</br>

**Important:** This repository does not contain the google-services.json file, which is needed for the app to communicate with Google Firebase and contains private API keys. To be able to run the application using Android Studio, you need to connect your own Firebase project.

*Note: The classes for daily reminder notifications and task notifications are in the source code, however, they are not being used in this release. They will be adapted to enhance user experience in the possible future updates.*

## Download the APK file
Download link: [Download Here](https://drive.google.com/file/d/1-bU4dlOYSaBN0P3zEy5hQFYYQKnuMMtX/view?usp=sharing)

VirusTotal results: [Check Results Here](https://www.virustotal.com/gui/file/0f070abf50b48819e318ef6246e7eeabd861b4ffc8ab892a924a0681e209f538/detection)

## Feedback & Questions
I'm open to all kinds of feedback and questions. This is my very first mobile app, which I developed only in 3-4 weeks including learning Kotlin and Android Studio from scratch. Thank you for testing and using the app, and supporting the development! Please contact me using my mail adresses given on my github profile for your questions.
