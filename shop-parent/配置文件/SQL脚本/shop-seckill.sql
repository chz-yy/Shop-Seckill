/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 50741
 Source Host           : localhost:3306
 Source Schema         : shop-seckill

 Target Server Type    : MySQL
 Target Server Version : 50741
 File Encoding         : 65001

 Date: 08/06/2023 18:50:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_order_info
-- ----------------------------
DROP TABLE IF EXISTS `t_order_info`;
CREATE TABLE `t_order_info`  (
  `order_no` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `user_id` bigint(20) NULL DEFAULT NULL,
  `product_id` bigint(20) NULL DEFAULT NULL,
  `product_img` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `delivery_addr_id` bigint(20) NULL DEFAULT NULL,
  `product_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `product_count` int(11) NULL DEFAULT NULL,
  `product_price` decimal(10, 2) NULL DEFAULT NULL,
  `seckill_price` decimal(10, 2) NULL DEFAULT NULL,
  `status` tinyint(4) NULL DEFAULT NULL,
  `create_date` datetime(0) NULL DEFAULT NULL,
  `pay_date` datetime(0) NULL DEFAULT NULL,
  `seckill_date` date NULL DEFAULT NULL,
  `seckill_time` int(11) NULL DEFAULT NULL,
  `intergral` decimal(10, 0) NULL DEFAULT NULL,
  `seckill_id` bigint(20) NULL DEFAULT NULL,
  `pay_type` tinyint(4) NULL DEFAULT NULL,
  PRIMARY KEY (`order_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_pay_log
-- ----------------------------
DROP TABLE IF EXISTS `t_pay_log`;
CREATE TABLE `t_pay_log`  (
  `trade_no` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `out_trade_no` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `notify_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `total_amount` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `pay_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`trade_no`, `out_trade_no`, `pay_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_refund_log
-- ----------------------------
DROP TABLE IF EXISTS `t_refund_log`;
CREATE TABLE `t_refund_log`  (
  `out_trade_no` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `refund_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `refund_amount` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `refund_reason` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `refund_type` tinyint(4) NULL DEFAULT NULL,
  PRIMARY KEY (`out_trade_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_seckill_product
-- ----------------------------
DROP TABLE IF EXISTS `t_seckill_product`;
CREATE TABLE `t_seckill_product`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `product_id` bigint(20) NULL DEFAULT NULL,
  `seckill_price` decimal(10, 2) NULL DEFAULT NULL,
  `intergral` decimal(10, 0) NULL DEFAULT NULL,
  `stock_count` int(255) NULL DEFAULT NULL,
  `start_date` date NULL DEFAULT NULL,
  `time` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_seckill_product
-- ----------------------------
INSERT INTO `t_seckill_product` VALUES (2, 23, 3699.00, 36990, 10, '2023-06-08', 10);
INSERT INTO `t_seckill_product` VALUES (3, 24, 2999.00, 29990, 10, '2023-06-08', 10);
INSERT INTO `t_seckill_product` VALUES (4, 25, 9.00, 90, 10, '2023-06-08', 10);
INSERT INTO `t_seckill_product` VALUES (5, 26, 899.00, 8990, 10, '2023-06-08', 10);
INSERT INTO `t_seckill_product` VALUES (6, 27, 59.00, 590, 10, '2023-06-08', 10);
INSERT INTO `t_seckill_product` VALUES (7, 28, 1699.00, 16990, 10, '2023-06-08', 10);
INSERT INTO `t_seckill_product` VALUES (8, 29, 3999.00, 3999, 10, '2023-06-08', 10);
INSERT INTO `t_seckill_product` VALUES (9, 30, 1099.00, 10990, 10, '2023-06-08', 10);
INSERT INTO `t_seckill_product` VALUES (10, 31, 2399.00, 23990, 10, '2023-06-08', 12);
INSERT INTO `t_seckill_product` VALUES (11, 32, 799.00, 7990, 10, '2023-06-08', 12);
INSERT INTO `t_seckill_product` VALUES (12, 33, 199.00, 1990, 10, '2023-06-08', 12);
INSERT INTO `t_seckill_product` VALUES (13, 34, 949.00, 9490, 10, '2023-06-08', 12);
INSERT INTO `t_seckill_product` VALUES (14, 35, 2499.00, 24990, 10, '2023-06-08', 12);
INSERT INTO `t_seckill_product` VALUES (15, 36, 1199.00, 11990, 10, '2023-06-08', 12);
INSERT INTO `t_seckill_product` VALUES (16, 37, 5999.00, 59990, 10, '2023-06-08', 12);
INSERT INTO `t_seckill_product` VALUES (17, 38, 9.00, 90, 10, '2023-06-08', 12);
INSERT INTO `t_seckill_product` VALUES (18, 39, 999.00, 9990, 10, '2023-06-08', 14);
INSERT INTO `t_seckill_product` VALUES (19, 40, 659.00, 6590, 10, '2023-06-08', 14);
INSERT INTO `t_seckill_product` VALUES (20, 41, 1999.00, 19990, 10, '2023-06-08', 14);
INSERT INTO `t_seckill_product` VALUES (21, 42, 3399.00, 33990, 10, '2023-06-08', 14);

-- ----------------------------
-- Table structure for undo_log
-- ----------------------------
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `context` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime(0) NOT NULL,
  `log_modified` datetime(0) NOT NULL,
  `ext` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `ux_undo_log`(`xid`, `branch_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
