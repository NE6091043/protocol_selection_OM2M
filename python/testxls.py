import xlsxwriter

workbook = xlsxwriter.Workbook('xxsxa.xlsx')
worksheet = workbook.add_worksheet()

# Start from the first cell.
# Rows and columns are zero indexed.

worksheet.write(0, 0, "sssssss")

row = 1
column = 0

content = ["ankit", "rahul", "priya", "harshita",
                    "sumit", "neeraj", "shivam"]

# iterating through content list
for item in content:

    # write operation perform
    worksheet.write(row, column, item)

    # incrementing the value of row by one
    # with each iterations.
    row += 1

workbook.close()
