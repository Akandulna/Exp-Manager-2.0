package com.example.util

import com.example.data.TransactionEntity

object SampleStatementCatalog {

    fun getGooglePayStatementTransactions(): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        var seq = 0L

        fun addDebit(date: String, time: String, payee: String, amount: Double, upiId: String, method: String) {
            seq += 1000L
            val parsedTs = DateUtils.parseToMillis(date, time) + seq
            val title = "Paid to $payee"
            val category = CategoryClassifier.classify(title, payee)
            list.add(
                TransactionEntity(
                    date = date,
                    time = time,
                    rawTimestamp = parsedTs,
                    title = title,
                    payee = payee,
                    amount = amount,
                    type = "DEBIT",
                    category = category,
                    upiTransactionId = upiId,
                    paymentMethod = method,
                    statementSource = "Google Pay PDF (Jul 2026)"
                )
            )
        }

        fun addCredit(date: String, time: String, sender: String, amount: Double, upiId: String, method: String) {
            seq += 1000L
            val parsedTs = DateUtils.parseToMillis(date, time) + seq
            val title = "Received from $sender"
            val category = CategoryClassifier.classify(title, sender)
            list.add(
                TransactionEntity(
                    date = date,
                    time = time,
                    rawTimestamp = parsedTs,
                    title = title,
                    payee = sender,
                    amount = amount,
                    type = "CREDIT",
                    category = category,
                    upiTransactionId = upiId,
                    paymentMethod = method,
                    statementSource = "Google Pay PDF (Jul 2026)"
                )
            )
        }

        // Page 1
        addDebit("01 Jul, 2026", "10:15 AM", "KAUSHAL KUMAR", 10.0, "654814283574", "Slice Bank 8941")
        addDebit("01 Jul, 2026", "10:15 AM", "BANARASI CHAURASIYA", 25.0, "654886681132", "Slice Bank 8941")
        addDebit("01 Jul, 2026", "11:37 AM", "NAGORI TEA TIME", 37.0, "654894273119", "Slice Bank 8941")
        addDebit("01 Jul, 2026", "02:20 PM", "Ramchndra Jayswal", 25.0, "618270117278", "Slice Bank 8941")
        addDebit("01 Jul, 2026", "07:38 PM", "SHIV SHANKAR SO SRI", 25.0, "618206138753", "Slice Bank 8941")
        addDebit("01 Jul, 2026", "10:39 PM", "GOVIND", 25.0, "618206277981", "Slice Bank 8941")
        addDebit("01 Jul, 2026", "11:50 PM", "SANDEEP CHAUHAN", 25.0, "618213276588", "Slice Bank 8941")
        addDebit("01 Jul, 2026", "11:52 PM", "KURBAN ALI", 10.0, "618262771396", "Slice Bank 8941")

        // Page 2
        addDebit("02 Jul, 2026", "10:48 AM", "RAMNAYAN YADAV", 25.0, "654926511806", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "10:49 AM", "MOHAMMAD HUSAIN", 10.0, "618355488504", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "11:06 AM", "ROPPEN TRANSPORTATION SERVICES", 82.0, "618367593970", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "12:19 PM", "ZEPTO MARKETPLACE", 300.0, "654907406550", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "02:50 PM", "Welcome electric and hardware", 20.0, "654932923250", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "05:26 PM", "Rapido", 43.0, "654991333264", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "05:57 PM", "GANESH SEAT MAKER", 550.0, "654966319471", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "07:47 PM", "SHIV DATT PANDEY", 20.0, "654965336512", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "08:09 PM", "Kolhapuri Tadka", 36.0, "654986530523", "Slice Bank 8941")

        // Page 3
        addDebit("02 Jul, 2026", "08:12 PM", "Mr MD SALAUDDIN", 25.0, "654985637043", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "08:32 PM", "Ramesh Patidar S O Ratanji Pat", 12.0, "654950340451", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "09:03 PM", "Mr MD SALAUDDIN", 25.0, "654915265994", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "09:41 PM", "Rajkamal Saree Centre", 140.0, "654933061275", "Slice Bank 8941")
        addDebit("02 Jul, 2026", "10:09 PM", "YOGESH RAMSHARAN CHAURASIA", 25.0, "654903669155", "Slice Bank 8941")
        addCredit("02 Jul, 2026", "10:19 PM", "Google Pay rewards", 2.0, "140907441836", "Canara Bank 8411")
        addDebit("03 Jul, 2026", "02:28 AM", "HEAVENLY SECRETS PRIVATE", 649.0, "655073371841", "Slice Bank 8941")
        addDebit("03 Jul, 2026", "12:00 PM", "ZEPTO MARKETPLACE", 240.0, "655030290159", "Slice Bank 8941")
        addDebit("03 Jul, 2026", "01:47 PM", "PAYU FINANCE INDIA", 1338.0, "618417626055", "Slice Bank 8941")

        // Page 4 & 5
        addDebit("03 Jul, 2026", "08:04 PM", "KAUSHAL KUMAR", 10.0, "618417253478", "Slice Bank 8941")
        addDebit("03 Jul, 2026", "08:04 PM", "BANARASI CHAURASIYA", 25.0, "618470545354", "Slice Bank 8941")
        addDebit("03 Jul, 2026", "09:07 PM", "SHIV DATT PANDEY", 20.0, "618465343203", "Slice Bank 8941")
        addDebit("03 Jul, 2026", "11:01 PM", "THE FOOD VAN", 180.0, "618420563607", "Slice Bank 8941")
        addDebit("04 Jul, 2026", "07:21 PM", "Shahid Mukesh Jadhav Petroleum", 1132.28, "655119934009", "Slice Bank 8941")
        addDebit("04 Jul, 2026", "11:39 PM", "PREMCHANDRA", 27.0, "655116751027", "Slice Bank 8941")
        addCredit("05 Jul, 2026", "08:02 PM", "Anne Lal", 663.0, "618649897737", "Canara Bank 8411")
        addDebit("05 Jul, 2026", "08:06 PM", "Prakash marandi", 1327.0, "618644742739", "Canara Bank 8411")
        addDebit("05 Jul, 2026", "08:53 PM", "GOLD COIN WINE MART", 400.0, "618600857576", "Slice Bank 8941")

        // Recent Pages (July 20-31)
        addDebit("23 Jul, 2026", "08:55 PM", "Shahid Mukesh Petroleum", 1194.0, "657096963320", "Slice Bank 8941")
        addDebit("24 Jul, 2026", "11:41 AM", "Blinkit", 678.0, "620542504221", "Slice Bank 8941")
        addDebit("24 Jul, 2026", "09:21 PM", "Living Liquidz CBD Belapur", 1080.0, "620542866201", "Slice Bank 8941")
        addDebit("27 Jul, 2026", "10:36 AM", "VIATERRA TRAVEL GEAR", 1849.0, "657435213655", "Slice Bank 8941")
        addDebit("28 Jul, 2026", "09:02 PM", "Dominos Pizza", 772.44, "657582702827", "Slice Bank 8941")
        addDebit("29 Jul, 2026", "11:27 AM", "Zepto Marketplace", 464.0, "657688122380", "Slice Bank 8941")
        addCredit("30 Jul, 2026", "01:06 AM", "PRAJWAL MISTRY", 100.0, "621155411185", "Canara Bank 8411")
        addDebit("30 Jul, 2026", "03:36 PM", "Blinkit", 366.0, "621140664039", "Slice Bank 8941")
        addDebit("31 Jul, 2026", "05:58 PM", "Blinkit", 430.0, "657838456531", "Slice Bank 8941")
        addDebit("31 Jul, 2026", "09:54 PM", "ARIHANT ELC HW STORES", 440.0, "657802898149", "Slice Bank 8941")

        return list.sortedByDescending { it.rawTimestamp }
    }

    fun getSliceStatementTransactions(): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        var seq = 0L

        fun addDebit(date: String, payee: String, amount: Double, refNo: String) {
            seq += 1000L
            val parsedTs = DateUtils.parseToMillis(date, "12:00 PM") + seq
            val title = "Paid to $payee"
            val category = CategoryClassifier.classify(title, payee)
            list.add(
                TransactionEntity(
                    date = date,
                    time = "12:00 PM",
                    rawTimestamp = parsedTs,
                    title = title,
                    payee = payee,
                    amount = amount,
                    type = "DEBIT",
                    category = category,
                    upiTransactionId = refNo,
                    paymentMethod = "Slice Small Finance Bank",
                    statementSource = "Slice Bank PDF (Aug 2026)"
                )
            )
        }

        fun addCredit(date: String, sender: String, amount: Double, refNo: String) {
            seq += 1000L
            val parsedTs = DateUtils.parseToMillis(date, "12:00 PM") + seq
            val title = "Credit / $sender"
            val category = CategoryClassifier.classify(title, sender)
            list.add(
                TransactionEntity(
                    date = date,
                    time = "12:00 PM",
                    rawTimestamp = parsedTs,
                    title = title,
                    payee = sender,
                    amount = amount,
                    type = "CREDIT",
                    category = category,
                    upiTransactionId = refNo,
                    paymentMethod = "Slice Small Finance Bank",
                    statementSource = "Slice Bank PDF (Aug 2026)"
                )
            )
        }

        // Slice Statement Aug 2026
        addCredit("01 Aug '26", "Interest Cr. for 31-Jul-2026", 36.78, "2022621355074")
        addCredit("01 Aug '26", "Deposit Interest Payout", 7.0, "2022621371094")
        addDebit("01 Aug '26", "ANIL KUMAR", 25.0, "20260801170766701")
        addDebit("01 Aug '26", "CHABIKRISHAN VITHAL", 12.0, "20260801172164201")
        addDebit("01 Aug '26", "CHABIKRISHAN VITHAL", 10.0, "20260801175306001")
        addDebit("01 Aug '26", "ASHEESH KUMAR", 24.0, "20260801420037001")
        addDebit("01 Aug '26", "BANARASI CHAURASIYA", 25.0, "20260801433259101")
        addCredit("02 Aug '26", "ABHISHEK KANDULNA Transfer", 60000.0, "2026080247182501")
        addDebit("02 Aug '26", "Petrol vamika energ", 1001.0, "20260802223821301")
        addDebit("02 Aug '26", "SSFRL 2", 450.0, "20260802240035201")
        addDebit("02 Aug '26", "SANDEEP CHAUHAN", 240.0, "20260802454754301")
        addDebit("02 Aug '26", "EatClub", 449.0, "20260802465335701")
        addDebit("03 Aug '26", "LAZY PAY HDFC", 1291.0, "20260803201625801")
        addDebit("03 Aug '26", "CRED Club", 4735.0, "20260803209135001")
        addDebit("03 Aug '26", "CRED Club", 2979.0, "20260803209539501")
        addDebit("05 Aug '26", "CREDIT SAISON INDIA", 9471.0, "20260805213557701")
        addDebit("05 Aug '26", "KRAZYBEE SERVICES", 11343.0, "20260805214617401")
        addDebit("05 Aug '26", "Repayment 621700103451", 4075.86, "202262171565914")
        addDebit("07 Aug '26", "SUDHANSU BHUJABAL", 7000.0, "202608075209501")
        addDebit("10 Aug '26", "Blinkit", 636.0, "202608101034901")
        addDebit("10 Aug '26", "SUDHANSU BHUJABAL", 5000.0, "20260810258032801")
        addDebit("10 Aug '26", "Ashish Service4", 750.0, "20260810450747001")
        addDebit("11 Aug '26", "ZEPTO MARKETPLACE", 277.0, "20260811167504801")
        addDebit("11 Aug '26", "OM SAIRAM ENTERPRISE - BharatPe", 2500.0, "20260811335251801")
        addDebit("11 Aug '26", "BIKERS HUB", 2700.0, "20260811392885801")

        return list.sortedByDescending { it.rawTimestamp }
    }

    fun getAnnualStatementTransactions(): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        var seq = 0L

        val months = listOf(
            Triple("Jan", 1, 31),
            Triple("Feb", 2, 28),
            Triple("Mar", 3, 31),
            Triple("Apr", 4, 30),
            Triple("May", 5, 31),
            Triple("Jun", 6, 30),
            Triple("Jul", 7, 31),
            Triple("Aug", 8, 11)
        )

        val dailyPayees = listOf(
            Triple("Swiggy Food", 280.0, "Food & Dining"),
            Triple("Zomato Online", 340.0, "Food & Dining"),
            Triple("Blinkit Groceries", 450.0, "Groceries"),
            Triple("Zepto Marketplace", 320.0, "Groceries"),
            Triple("Rapido Bike Taxi", 65.0, "Transport & Fuel"),
            Triple("Uber India", 210.0, "Transport & Fuel"),
            Triple("Shell Petroleum", 1200.0, "Transport & Fuel"),
            Triple("DMart Ready", 890.0, "Groceries"),
            Triple("Tea Time Cafe", 35.0, "Food & Dining"),
            Triple("Amazon India", 1450.0, "Shopping & Health"),
            Triple("Flipkart Internet", 990.0, "Shopping & Health"),
            Triple("Chai Point", 45.0, "Food & Dining"),
            Triple("Apollo Pharmacy", 380.0, "Shopping & Health"),
            Triple("Local Bakery & Sweets", 120.0, "Food & Dining"),
            Triple("Subway India", 290.0, "Food & Dining"),
            Triple("Airtel Mobile Recharge", 299.0, "Bills & Utilities"),
            Triple("Jio Fiber Internet", 825.0, "Bills & Utilities"),
            Triple("CRED Club Card Bill", 2450.0, "Finance & Bills"),
            Triple("Coffee Day", 180.0, "Food & Dining"),
            Triple("BharatPe Merchant", 150.0, "Personal & Others")
        )

        val creditSenders = listOf(
            Triple("TECH CORP SALARY", 85000.0, "Income & Cashbacks"),
            Triple("FREELANCE CLIENT CONSULTING", 15000.0, "Income & Cashbacks"),
            Triple("Google Pay Cash Reward", 15.0, "Income & Cashbacks"),
            Triple("PRAJWAL MISTRY Refund", 250.0, "Transfers & Savings"),
            Triple("Dividends / Mutual Fund Cr", 450.0, "Income & Cashbacks")
        )

        months.forEach { (monthStr, monthIndex, maxDays) ->
            for (day in 1..maxDays) {
                val dayStr = if (day < 10) "0$day" else "$day"
                val dateStr = "$dayStr $monthStr, 2026"

                // 1st of month: Salary
                if (day == 1) {
                    seq += 1000L
                    val time = "09:00 AM"
                    val parsedTs = DateUtils.parseToMillis(dateStr, time) + seq
                    list.add(
                        TransactionEntity(
                            date = dateStr,
                            time = time,
                            rawTimestamp = parsedTs,
                            title = "Salary Credit / TECH CORP",
                            payee = creditSenders[0].first,
                            amount = creditSenders[0].second,
                            type = "CREDIT",
                            category = creditSenders[0].third,
                            upiTransactionId = "20260${monthIndex}01${seq}",
                            paymentMethod = "HDFC Bank Direct Deposit",
                            statementSource = "Full Year Statement 2026 (1,200+ Tx)"
                        )
                    )
                }

                // 5th of month: Rent
                if (day == 5) {
                    seq += 1000L
                    val time = "10:30 AM"
                    val parsedTs = DateUtils.parseToMillis(dateStr, time) + seq
                    list.add(
                        TransactionEntity(
                            date = dateStr,
                            time = time,
                            rawTimestamp = parsedTs,
                            title = "Paid to House Rent / Landlord",
                            payee = "House Rent Landlord",
                            amount = 18000.0,
                            type = "DEBIT",
                            category = "Finance & Bills",
                            upiTransactionId = "20260${monthIndex}05${seq}",
                            paymentMethod = "Axis Bank NetBanking",
                            statementSource = "Full Year Statement 2026 (1,200+ Tx)"
                        )
                    )
                }

                // 15th of month: Freelance Income
                if (day == 15) {
                    seq += 1000L
                    val time = "04:15 PM"
                    val parsedTs = DateUtils.parseToMillis(dateStr, time) + seq
                    list.add(
                        TransactionEntity(
                            date = dateStr,
                            time = time,
                            rawTimestamp = parsedTs,
                            title = "Received from Client Consulting",
                            payee = creditSenders[1].first,
                            amount = creditSenders[1].second,
                            type = "CREDIT",
                            category = creditSenders[1].third,
                            upiTransactionId = "20260${monthIndex}15${seq}",
                            paymentMethod = "UPI Transfer",
                            statementSource = "Full Year Statement 2026 (1,200+ Tx)"
                        )
                    )
                }

                // 5 daily transactions per day
                val seed = (monthIndex * 31 + day)
                for (t in 0..4) {
                    seq += 1000L
                    val payeeInfo = dailyPayees[(seed + t * 3) % dailyPayees.size]
                    val hour = (8 + (t * 3)) % 12 + 1
                    val minute = (t * 17) % 60
                    val amPm = if (t >= 2) "PM" else "AM"
                    val timeStr = String.format("%02d:%02d %s", hour, minute, amPm)

                    val parsedTs = DateUtils.parseToMillis(dateStr, timeStr) + seq
                    val amountVariation = payeeInfo.second + ((seed * (t + 1)) % 40) - 20

                    list.add(
                        TransactionEntity(
                            date = dateStr,
                            time = timeStr,
                            rawTimestamp = parsedTs,
                            title = "Paid to ${payeeInfo.first}",
                            payee = payeeInfo.first,
                            amount = if (amountVariation > 5) amountVariation else payeeInfo.second,
                            type = "DEBIT",
                            category = payeeInfo.third,
                            upiTransactionId = "20260${monthIndex}${dayStr}${t}${seq % 10000}",
                            paymentMethod = if (t % 2 == 0) "Slice Bank 8941" else "GPay / Canara Bank",
                            statementSource = "Full Year Statement 2026 (1,200+ Tx)"
                        )
                    )
                }
            }
        }

        return list.sortedByDescending { it.rawTimestamp }
    }
}
