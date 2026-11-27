require 'net/http'
require 'base64'

module Api
  class AccountsController < ApplicationController
    def avatar
      response = Net::HTTP.get_response(URI(params[:url].to_s))
      return render json: { error: 'download failed' }, status: :bad_request unless response.is_a?(Net::HTTPSuccess)

      current_user.update!(avatar: Base64.strict_encode64(response.body))
      render json: { saved: true }
    end

    def cart
      items = Marshal.load(Base64.decode64(params[:cart_state].to_s))
      render json: { items: items.respond_to?(:size) ? items.size : 0 }
    end

    def import
      feed = open(params[:source].to_s).read
      count = feed.lines.count { |line| line.strip.present? }
      render json: { imported: count }
    end
  end
end
