module Api
  class OrdersController < ApplicationController
    def payment
      order = Order.where(id: params[:id], user_id: current_user.id).first
      return head :not_found if order.nil?

      render json: PaymentClient.new.status(order.payment_id)
    end
  end
end
