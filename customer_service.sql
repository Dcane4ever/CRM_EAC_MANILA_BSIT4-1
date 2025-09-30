-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Sep 09, 2025 at 02:00 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `customer_service`
--

-- --------------------------------------------------------

--
-- Table structure for table `help_topic`
--

CREATE TABLE `help_topic` (
  `id` bigint(20) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `help_topic`
--

INSERT INTO `help_topic` (`id`, `description`, `title`, `url`) VALUES
(1, 'Step-by-step guide to reset your TechCorp account password.', 'How to reset your password', '/faq#reset-password'),
(2, 'Find your order status and shipping information.', 'Track your order', '/faq#track-order'),
(3, 'Ways to reach our technical support team for device issues.', 'Contact technical support', '/faq#contact-support'),
(4, 'Best practices for keeping your account safe.', 'Account security tips', '/faq#security-tips'),
(5, 'Instructions to update your registered email address.', 'Change your email address', '/faq#change-email'),
(6, 'Learn about our refund and return policies.', 'Refund policy details', '/faq#refund-policy'),
(7, 'Common solutions for login problems.', 'Troubleshooting login issues', '/faq#login-troubleshoot'),
(8, 'All available support channels and contact options.', 'How to contact support', '/faq#contact-options');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL,
  `available` tinyint(1) NOT NULL DEFAULT 1,
  `guest` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users` (sample data)
--

INSERT INTO `users` (`id`, `username`, `password`, `email`, `role`, `available`, `guest`) VALUES
(1, 'admin', '$2a$10$7e3.KbQPWcYv8ZcI0x1kAeqKjFj8tFjHhF4U8XIGM8Y7wQO1Y1O9S', 'admin@techcorp.com', 'ADMIN', 1, 0),
(2, 'agent1', '$2a$10$7e3.KbQPWcYv8ZcI0x1kAeqKjFj8tFjHhF4U8XIGM8Y7wQO1Y1O9S', 'agent1@techcorp.com', 'AGENT', 1, 0),
(3, 'customer1', '$2a$10$7e3.KbQPWcYv8ZcI0x1kAeqKjFj8tFjHhF4U8XIGM8Y7wQO1Y1O9S', 'customer1@email.com', 'CUSTOMER', 1, 0);

-- --------------------------------------------------------

--
-- Table structure for table `chat_sessions`
--

CREATE TABLE `chat_sessions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `customer_id` bigint(20) DEFAULT NULL,
  `agent_id` bigint(20) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'WAITING',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ended_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_customer` (`customer_id`),
  KEY `fk_agent` (`agent_id`),
  FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  FOREIGN KEY (`agent_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `chat_messages`
--

CREATE TABLE `chat_messages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` bigint(20) NOT NULL,
  `sender_id` bigint(20) DEFAULT NULL,
  `message_type` varchar(20) NOT NULL DEFAULT 'CHAT',
  `content` text NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_session` (`session_id`),
  KEY `fk_sender` (`sender_id`),
  FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `help_topic_steps`
--

CREATE TABLE `help_topic_steps` (
  `help_topic_id` bigint(20) NOT NULL,
  `steps` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `help_topic_steps`
--

INSERT INTO `help_topic_steps` (`help_topic_id`, `steps`) VALUES
(1, 'Go to the login page and click \"Forgot Password\".'),
(1, 'Enter your registered email address.'),
(1, 'Check your email for a password reset link.'),
(1, 'Click the link and enter a new password.'),
(1, 'Log in with your new password.'),
(2, 'Log into your account or go to the order tracking page.'),
(2, 'Enter your order number and email address used for the purchase.'),
(2, 'Click \"Track Order\" to view current status.'),
(2, 'Check the tracking information for estimated delivery date.'),
(2, 'Contact support if tracking shows any issues or delays.'),
(3, 'Visit the technical support section on our website.'),
(3, 'Choose your product category from the dropdown menu.'),
(3, 'Select the specific issue you are experiencing.'),
(3, 'Fill out the support form with detailed information about your problem.'),
(3, 'Attach any relevant screenshots or error messages.'),
(3, 'Submit the form and note your support ticket number for reference.'),
(4, 'Log into your account and go to Account Settings.'),
(4, 'Click on \"Security\" or \"Privacy Settings\".'),
(4, 'Review your current security settings and recent login activity.'),
(4, 'Update your password if you suspect unauthorized access.'),
(4, 'Enable two-factor authentication for added security.'),
(4, 'Review and update your recovery email and phone number.'),
(5, 'Log into your account and navigate to Account Settings.'),
(5, 'Click on \"Personal Information\" or \"Profile Settings\".'),
(5, 'Locate the email address field and click \"Edit\" or \"Change\".'),
(5, 'Enter your new email address in the provided field.'),
(5, 'Verify the change by clicking the confirmation link sent to your new email.'),
(5, 'Log in again using your new email address to confirm the change.'),
(6, 'Review our refund policy on the website or in your purchase confirmation.'),
(6, 'Ensure your request falls within the eligible timeframe for refunds.'),
(6, 'Gather your order information including order number and purchase date.'),
(6, 'Contact customer service through the refund request form.'),
(6, 'Provide reason for refund and any supporting documentation.'),
(6, 'Wait for approval and follow instructions for returning the item if applicable.'),
(7, 'Clear your browser cache and cookies.'),
(7, 'Try logging in using a different browser or incognito/private mode.'),
(7, 'Verify you are using the correct email address and password.'),
(7, 'Check if Caps Lock is on and ensure correct capitalization.'),
(7, 'Try resetting your password if login attempts continue to fail.'),
(7, 'Contact technical support if the issue persists after trying these steps.'),
(8, 'Visit the \"Contact Us\" or \"Support\" section of our website.'),
(8, 'Choose the most appropriate contact method for your issue (chat, email, phone).'),
(8, 'For live chat: click the chat icon and wait to be connected with an agent.'),
(8, 'For email support: fill out the contact form with detailed information.'),
(8, 'For phone support: call during business hours and have your account information ready.'),
(8, 'Save any reference numbers or ticket IDs provided for future follow-up.');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `help_topic`
--
ALTER TABLE `help_topic`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `help_topic_steps`
--
ALTER TABLE `help_topic_steps`
  ADD KEY `FKjb7agi5vu58d70lelr306g12m` (`help_topic_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `help_topic`
--
ALTER TABLE `help_topic`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `help_topic_steps`
--
ALTER TABLE `help_topic_steps`
  ADD CONSTRAINT `FKjb7agi5vu58d70lelr306g12m` FOREIGN KEY (`help_topic_id`) REFERENCES `help_topic` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
