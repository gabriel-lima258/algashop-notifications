TRUNCATE TABLE email_template CASCADE;

insert into email_template (id, name, subject, body, created_at, updated_at, version)
values (
    '0198d0a1-2c44-7d0b-8e11-9a0b7c5d3f21',
    'Order Placed',
    'Order [[${orderId}]] received',
    $body$<!DOCTYPE html>
<html>
<body style="font-family: Arial, Helvetica, sans-serif; color: #222;">
  <h2>Hello, <span th:text="${recipient.name}">customer</span>!</h2>
  <p>
    We have received your order <strong th:text="${orderId}">ORDER-ID</strong>
    on <span th:text="|${#strings.substring(placedAt, 8, 10)}/${#strings.substring(placedAt, 5, 7)}/${#strings.substring(placedAt, 0, 4)}|">01/01/2000</span>.
  </p>
  <table style="border-collapse: collapse; width: 100%;">
    <thead>
      <tr>
        <th style="text-align: left; border-bottom: 1px solid #ccc;">Product</th>
        <th style="text-align: right; border-bottom: 1px solid #ccc;">Qty.</th>
        <th style="text-align: right; border-bottom: 1px solid #ccc;">Price</th>
        <th style="text-align: right; border-bottom: 1px solid #ccc;">Total</th>
      </tr>
    </thead>
    <tbody>
      <tr th:each="item : ${items}">
        <td th:text="${item.productName}">Product</td>
        <td style="text-align: right;" th:text="${item.quantity}">1</td>
        <td style="text-align: right;" th:text="${#numbers.formatCurrency(item.price)}">$0.00</td>
        <td style="text-align: right;" th:text="${#numbers.formatCurrency(item.totalAmount)}">$0.00</td>
      </tr>
    </tbody>
  </table>
  <p>Shipping: <span th:text="${#numbers.formatCurrency(shippingCost)}">$0.00</span></p>
  <p><strong>Order total: <span th:text="${#numbers.formatCurrency(totalAmount)}">$0.00</span></strong></p>
  <p>Estimated delivery: <span th:text="|${#strings.substring(expectedDeliveryDate, 8, 10)}/${#strings.substring(expectedDeliveryDate, 5, 7)}/${#strings.substring(expectedDeliveryDate, 0, 4)}|">01/01/2000</span></p>
  <p>Thank you for shopping at AlgaShop!</p>
</body>
</html>
$body$,
    now(),
    now(),
    0
);