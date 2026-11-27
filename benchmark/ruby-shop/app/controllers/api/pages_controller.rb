module Api
  class PagesController < ApplicationController
    skip_before_action :authenticate_user!

    RECOMMENDATIONS = %w[coffee tea cocoa juice].freeze

    def search
      render html: "<h1>Results for #{params[:q]}</h1>".html_safe
    end

    def safe_search
      render html: "<h1>Results for #{ERB::Util.html_escape(params[:q])}</h1>".html_safe
    end

    def recommendations
      render json: { pick: RECOMMENDATIONS[rand(RECOMMENDATIONS.size)] }
    end
  end
end
