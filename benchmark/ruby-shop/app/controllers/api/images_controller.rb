module Api
  class ImagesController < ApplicationController
    skip_before_action :authenticate_user!

    def show
      send_file Rails.root.join('storage', 'images', params[:file].to_s), type: 'image/png', disposition: 'inline'
    end

    def thumb
      name = File.basename(params[:file].to_s)
      send_file Rails.root.join('storage', 'thumbs', name), type: 'image/png', disposition: 'inline'
    end
  end
end
