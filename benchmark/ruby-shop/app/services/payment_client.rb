require 'net/http'
require 'json'

class PaymentClient
  def status(payment_id)
    body = Net::HTTP.get(URI("#{ENV.fetch('PAYMENTS_URL')}/v1/status/#{ERB::Util.url_encode(payment_id)}"))
    JSON.parse(body)
  end
end
